from __future__ import annotations

import json
import logging
from datetime import date
from typing import Any, Dict

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate

from app.chatbot.schemas import ChatbotIntent, ChatbotParsedSpec
from app.chatbot.services.agent_core.date_range import today_kst

logger = logging.getLogger(__name__)


_SYSTEM_PROMPT = """[Role]
너는 취업 공고 통계용 챗봇의 '질문 파서(parser)'다.
출력은 서버가 DB 조회 파라미터로만 사용한다.

[Rules]
- 추측 금지.
- intent는 가능하면 반드시 아래 [Supported intents] 중 하나로 선택하라.
- intent 자체가 전혀 판단되지 않을 때만 HELP.
- 날짜는 YYYY-MM-DD. end_date는 exclusive(미포함).
- 날짜/필터가 불확실하면 비워두고(Null/empty), validate 단계에서 기본값/상속이 처리된다.
- keywords_all: AND, keywords_any: OR
- industries_any: 업종/산업(OR). 예: ['it','ai','데이터 서비스']
- regions_any: 광역 코드(OR):
  SEOUL,BUSAN,DAEGU,INCHEON,GWANGJU,DAEJEON,ULSAN,SEJONG,
  GYEONGGI,GANGWON,CHUNGBUK,CHUNGNAM,JEONBUK,JEONNAM,
  GYEONGBUK,GYEONGNAM,JEJU
- admin_areas_any: 시/군/구(OR) 예: 강남구, 수원시, 성남시, 기장군
- min_salary_m만원 / max_salary_m만원
- min_competition_pct / max_competition_pct:
  공고별 경쟁률(%) = apply_count / recruitment_capacity * 100
  예) '경쟁률 120% 이하' => max_competition_pct=120
- min_required_experience_years / max_required_experience_years:
  정수(년). 둘 다 있으면 between(inclusive), min만 있으면 >=, max만 있으면 <=
  '신입'=0, '주니어'=1~3, '시니어'=3~5, '미들'=6~
- positions_any: 포지션 필터(OR). 반드시 아래 UI 라벨 중에서만 선택:
  전체, 서버/백엔드, 프론트엔드, 웹 풀스택, 안드로이드, iOS, 크로스플랫폼, 머신러닝/AI,
  데이터 엔지니어, 데이터 분석가, 데이터 사이언티스트, DevOps, 시스템 엔지니어,
  클라우드 엔지니어, DBA, SRE, 보안 엔지니어, 게임 클라이언트, 게임 서버, 임베디드,
  시스템 프로그래머, QA 엔지니어, 기술 PM, 프로덕트 매니저, UX/UI 디자이너, 블록체인
  (포지션 필터는 공고 title이 아니라 stack 키워드 매핑으로 처리됨)
- job_role: 자유 문자열(예: Backend, Frontend, Data Engineer 등). 포지션 UI 라벨로 확정 가능한 경우엔 positions_any를 우선 사용.
- limit: 1..20
- TOP_STACKS/BOTTOM_STACKS 질문에서 사용자가 개수를 명시하지 않으면 limit은 비워둔다(validate에서 기본값=5 적용).
- random: LIST_POSTINGS에서 랜덤 여부
- 리스트 필드는 비어도 반드시 []로 출력, null 금지

[Canonicalization]
- 기술스택/업종/직군/툴 이름은 가능한 표준 표기로 정규화하라.
  예: "스프링부트", "spring boot" -> "Spring Boot"
      "js" -> "JavaScript"
  확신이 없으면 사용자가 쓴 원문을 그대로 사용.

[Multi-turn]
- 기본적으로 '그러면/그렇다면/그럼/글면(…)' 등 전환어는 **새 질문**이다. (이전 필터/컨텍스트 상속 X)
- 단, '그중에서/그 중에서/거기서/거기에서/이어서/그 공고들중에서(…)' 등
  **이어가기 마커**가 포함된 경우에만 후속질의로 보고,
  [Conversation context]의 이전 parsed spec을 참고해 누락 필드를 채울 수 있다.
- 그래도 불확실하면 HELP.

[Supported intents]
1) COUNT_POSTINGS
2) COMPETITION
3) LIST_POSTINGS (최신/랜덤)
4) TOP_SALARY_POSTINGS (연봉 상위 공고)
5) BOTTOM_SALARY_POSTINGS (연봉 하위 공고)
6) TOP_STACKS (많이 등장한 스택)
7) BOTTOM_STACKS (적게 등장한 스택)
8) RATE_STATS (지원률/경쟁률 % 통계)
9) BUSIEST_WEEK
10) BUSIEST_DAY
11) LOW_COMPETITION_POSTINGS
12) HIGH_COMPETITION_POSTINGS
13) LOW_STACK_APPLY_RATE
14) HIGH_STACK_APPLY_RATE
15) TOP_INDUSTRIES
16) BOTTOM_INDUSTRIES
17) MOST_APPLICANTS_POSTINGS
18) DETAIL_URLS (직전 결과 상세 페이지 URL)
19) HELP
"""


def _examples(today_iso: str, year: int) -> str:
    return f"""[Today]
{today_iso}

[Examples]
- '오늘 공고 몇개 올라왔어?' => COUNT_POSTINGS, start_date=today, end_date=today+1
- '올해 8월 공고 몇개' => COUNT_POSTINGS, start_date={year}-08-01, end_date={year}-09-01
- '백엔드 자바/스프링부트 공고 5개 최신' => LIST_POSTINGS, positions_any=['서버/백엔드'], keywords_all=['Java','Spring Boot'], limit=5, random=false
- '서울 강남구 연봉 4천 이상 공고 5개 랜덤' => LIST_POSTINGS, regions_any=['SEOUL'], admin_areas_any=['강남구'], min_salary_m만원=4000, limit=5, random=true
- '연봉이 제일 높은 공고 5개' => TOP_SALARY_POSTINGS, limit=5
- '그중에서 연봉이 제일 낮은 공고 5개' => BOTTOM_SALARY_POSTINGS, limit=5
- '1월 9일 뒤로 연봉 8천~9천 공고 몇개' => COUNT_POSTINGS, start_date={year}-01-09, end_date=today+1, min=8000, max=9000
- '경기도 요즘 평균 지원률/경쟁률' => RATE_STATS, regions_any=['GYEONGGI']
- '요즘 올라오는 공고에서 제일 많이 요구하는 스택 5개' => TOP_STACKS, limit=5
- '요즘 올라오는 공고에서 제일 적게 요구하는 스택 5개' => BOTTOM_STACKS, limit=5
- '경쟁률 120% 이하 공고 몇개' => COUNT_POSTINGS, max_competition_pct=120
- '신입 개발자 모집하는 공고는 몇개야?' => COUNT_POSTINGS, min_required_experience_years=0, max_required_experience_years=0
- '경력이 3~5년 정도를 뽑는 공고 몇개야?' => COUNT_POSTINGS, min_required_experience_years=3, max_required_experience_years=5
- '산업분야 중 IT 관련 공고는 몇개야?' => COUNT_POSTINGS, industries_any=['it']
- 'AI, 데이터 서비스 업종 공고 몇개야?' => COUNT_POSTINGS, industries_any=['ai','데이터 서비스']
"""


def build_parse_prompt(parser: PydanticOutputParser, *, today_iso: str, year: int) -> ChatPromptTemplate:
    examples = _examples(today_iso, year)

    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", _SYSTEM_PROMPT + "\n\n{format_instructions}"),
            (
                "user",
                examples
                + "\n\n[Conversation context]\n{context}\n"
                + "\n[User question]\n{message}",
            ),
        ]
    ).partial(format_instructions=parser.get_format_instructions())

    return prompt


async def parse_message(
    llm,
    parser: PydanticOutputParser,
    *,
    message: str,
    last_parsed_dict: Dict[str, Any] | None,
) -> ChatbotParsedSpec:
    """LLM-based parse.

    On failures, retries once and falls back to HELP intent.
    """

    today_d: date = today_kst()
    today_iso = today_d.isoformat()
    year = int(today_d.year)

    context_dict = last_parsed_dict or {}
    context_json = json.dumps(context_dict, ensure_ascii=False) if context_dict else ""

    prompt = build_parse_prompt(parser, today_iso=today_iso, year=year)
    chain = prompt | llm | parser

    payload = {"message": message, "context": context_json}

    try:
        return await chain.ainvoke(payload)
    except Exception:
        logger.exception("chatbot parse failed (1st)")
        raw = await (prompt | llm).ainvoke(payload)
        logger.error("LLM raw output: %s", getattr(raw, "content", raw))
        try:
            return await chain.ainvoke(payload)
        except Exception:
            logger.exception("chatbot parse failed (2nd)")
            return ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)
