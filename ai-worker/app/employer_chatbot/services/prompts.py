# app/employer_chatbot/services/prompts.py
"""
기업용 채용 챗봇 프롬프트 모듈
- 시스템 프롬프트, 예시, 파싱 프롬프트 관리
"""
from __future__ import annotations

import json
import logging
from datetime import date
from typing import Any, Dict

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate

from app.employer_chatbot.schemas import EmployerChatbotIntent, EmployerChatbotParsedSpec

logger = logging.getLogger(__name__)


# =============================================================================
# 시스템 프롬프트
# =============================================================================

SYSTEM_PROMPT = """[역할]
너는 **기업 채용 담당자 전용 AI 어시스턴트**다.
기업 담당자가 자사 채용공고의 지원자 현황, 통계, 성과를 조회하고 분석할 수 있도록 돕는다.
출력은 서버가 DB 조회 파라미터로만 사용한다.

[핵심 원칙]
1. **추측 금지** - 불확실한 정보는 파싱하지 않고 비워둔다
2. **Intent 우선 확정** - 반드시 아래 [지원 Intent] 중 하나로 결정
3. **날짜 형식** - YYYY-MM-DD, end_date는 exclusive(미포함)
4. **후속 질문 처리** - '그 중에/거기서/이전' 등은 컨텍스트 상속

[지원 Intent]
1) COUNT_APPLICATIONS
   - 지원자 수 카운트
   - 예: "이번달 지원자 몇명?" "백엔드 공고에 얼마나 지원했어?"

2) LIST_APPLICATIONS
   - 지원자 목록 조회
   - 예: "최근 지원자 목록 보여줘" "서류 통과한 사람들 누구야?"

3) APPLICATION_STATS
   - 지원 통계 (상태별/공고별 분포)
   - 예: "지원 현황 분석해줘" "상태별 지원자 분포는?"

4) APPLICANT_TREND
   - 지원자 추이 분석 (일별/주별 변화)
   - 예: "이번달 지원자 추이는?" "주간별 지원자 변화"

5) POSTING_PERFORMANCE
   - 공고 성과 분석 (지원수, 경쟁률, 조회수)
   - 예: "우리 공고 성과 어때?" "어떤 공고가 가장 인기있어?"

6) COMPARE_POSTINGS
   - 공고 간 비교 분석
   - 예: "백엔드와 프론트엔드 공고 비교해줘"

7) TOP_SKILLS
   - 지원자들이 보유한 스킬 상위
   - 예: "지원자들이 가장 많이 보유한 스킬은?"

8) APPLICANT_PROFILE
   - 지원자 프로필 요약/분석
   - 예: "지원자들 평균 경력은?" "지원자 프로필 분석해줘"

9) CONVERSION_RATE
   - 채용 퍼널 전환율 (지원→서류→면접→합격)
   - 예: "채용 전환율 어때?" "합격률은 얼마야?"

10) URGENT_ACTIONS
    - 긴급 처리 필요 건
    - 예: "긴급하게 처리해야 할 건?" "오래된 대기 건 있어?"

11) PENDING_REVIEW
    - 검토 대기 중인 지원자
    - 예: "아직 검토 안한 지원자 몇명?"

12) HELP
    - 도움말/사용법

[필드 설명]
- job_posting_ids: 특정 공고 ID 필터 (정수 배열)
- job_title_keywords: 공고 제목 키워드 필터 (예: ['백엔드', '프론트엔드'])
- applicant_skills_any: 지원자 스킬 OR 필터 (하나라도 포함)
- applicant_skills_all: 지원자 스킬 AND 필터 (모두 포함)
- application_status_any: 지원 상태 필터
  * PENDING - 대기 중
  * REVIEWED - 검토 완료
  * SHORTLISTED - 서류 통과
  * INTERVIEW - 면접 진행
  * REJECTED - 불합격
  * HIRED - 합격/채용
- min_experience_years / max_experience_years: 경력 필터 (년 단위)
- regions_any: 지역 필터 (SEOUL, BUSAN, GYEONGGI 등)
- sort_by: 정렬 기준 (created_at, experience, salary, name)
- group_by: 그룹 기준 (day, week, month, status, posting)
- time_unit: 추이 분석 시 시간 단위 (day, week, month)
- limit: 결과 개수 제한 (1~50)
- random: 랜덤 여부

[지원 상태 한글 매핑]
- '대기', '미검토', '접수' → PENDING
- '검토완료', '확인완료' → REVIEWED
- '서류통과', '서류합격', '1차합격' → SHORTLISTED
- '면접', '면접진행', '면접예정' → INTERVIEW
- '불합격', '탈락' → REJECTED
- '합격', '채용', '최종합격' → HIRED

[Multi-turn 규칙]
1. '그 중에서/그중에/거기서/이전/방금/해당' 포함 시 → 후속 질문
   - 이전 컨텍스트의 날짜/필터를 상속
2. '그러면/그럼' 만 있으면 → 새 질문일 수 있음 (문맥 판단)
3. 불확실하면 HELP

[경력 표현 매핑]
- '신입' → min=0, max=0
- '주니어' → min=0, max=3
- '미들' → min=3, max=6
- '시니어' → min=5, max=10+
"""


def build_examples(today_iso: str, year: int) -> str:
    """예시 문장 생성"""
    return f"""[Today]
{today_iso}

[질문 → 파싱 예시]
1. '오늘 지원자 몇명이야?'
   → COUNT_APPLICATIONS, start_date={today_iso}, end_date=today+1

2. '이번달 지원 현황 보여줘'
   → APPLICATION_STATS, start_date={year}-{int(today_iso[5:7]):02d}-01

3. '백엔드 공고에 지원한 사람들 목록'
   → LIST_APPLICATIONS, job_title_keywords=['백엔드']

4. '서류 통과한 지원자 10명만'
   → LIST_APPLICATIONS, application_status_any=['SHORTLISTED'], limit=10

5. 'PENDING 상태 지원자 몇명?'
   → COUNT_APPLICATIONS, application_status_any=['PENDING']

6. '우리 공고 성과 분석해줘'
   → POSTING_PERFORMANCE

7. '지원자들이 많이 보유한 스킬 TOP 5'
   → TOP_SKILLS, limit=5

8. '이번주 지원자 추이는?'
   → APPLICANT_TREND, time_unit=week

9. '채용 전환율 어때?'
   → CONVERSION_RATE

10. '아직 검토 안한 지원자 있어?'
    → PENDING_REVIEW

11. '백엔드와 프론트엔드 공고 비교해줘'
    → COMPARE_POSTINGS, job_title_keywords=['백엔드', '프론트엔드']

12. '신입 지원자만 보여줘'
    → LIST_APPLICATIONS, min_experience_years=0, max_experience_years=0

13. '경력 3년 이상 지원자 몇명?'
    → COUNT_APPLICATIONS, min_experience_years=3

14. '최근 3일간 지원자 수'
    → COUNT_APPLICATIONS, start_date=today-3, end_date=today+1
"""


def build_parse_prompt(
    parser: PydanticOutputParser,
    *,
    today_iso: str,
    year: int
) -> ChatPromptTemplate:
    """LLM 파싱 프롬프트 생성"""
    examples = build_examples(today_iso, year)

    prompt = ChatPromptTemplate.from_messages([
        ("system", SYSTEM_PROMPT + "\n\n{format_instructions}"),
        (
            "user",
            examples
            + "\n\n[이전 대화 컨텍스트]\n{context}\n"
            + "\n[사용자 질문]\n{message}"
        ),
    ]).partial(format_instructions=parser.get_format_instructions())

    return prompt


async def parse_message(
    llm,
    parser: PydanticOutputParser,
    *,
    message: str,
    last_parsed_dict: Dict[str, Any] | None,
    today: date,
) -> EmployerChatbotParsedSpec:
    """
    LLM 기반 메시지 파싱
    실패 시 1회 재시도 후 HELP fallback
    """
    today_iso = today.isoformat()
    year = today.year

    context_dict = last_parsed_dict or {}
    context_json = json.dumps(context_dict, ensure_ascii=False) if context_dict else ""

    prompt = build_parse_prompt(parser, today_iso=today_iso, year=year)
    chain = prompt | llm | parser

    payload = {"message": message, "context": context_json}

    try:
        return await chain.ainvoke(payload)
    except Exception:
        logger.exception("employer chatbot parse failed (1st attempt)")
        # 디버깅용 로그
        raw = await (prompt | llm).ainvoke(payload)
        logger.error("LLM raw output: %s", getattr(raw, "content", raw))
        
        try:
            return await chain.ainvoke(payload)
        except Exception:
            logger.exception("employer chatbot parse failed (2nd attempt)")
            return EmployerChatbotParsedSpec(
                intent=EmployerChatbotIntent.HELP,
                confidence=0.0
            )


# =============================================================================
# 응답 생성용 프롬프트 (선택적 LLM 응답 생성 시)
# =============================================================================

RESPONSE_SYSTEM_PROMPT = """너는 친절하고 전문적인 기업 채용 담당자 어시스턴트다.

[원칙]
1. 데이터를 명확하고 간결하게 전달
2. 액션 가능한 인사이트 제공
3. 필요시 추가 질문 유도
4. 존댓말 사용

[응답 형식]
- 숫자는 포맷팅 (예: 1,234명)
- 비율은 % 표시
- 목록은 번호 매기기
- 중요 정보는 강조
"""
