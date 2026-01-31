# app/chatbot/services/agent.py
from __future__ import annotations

import calendar
import json
import logging
import re
import uuid
from datetime import date, datetime, timedelta
from typing import Any, Dict, List, Optional, TypedDict

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langgraph.graph import END, StateGraph
from zoneinfo import ZoneInfo

from app.chatbot.repository import job_stats_repo
from app.chatbot.schemas import ChatbotIntent, ChatbotParsedSpec
from app.chatbot.services.memory import (
    ChatbotConversationState,
    ConversationMode,
    RedisChatbotMemoryStore,
    TranscriptItem,
)
from app.chatbot.utils.industry import infer_industries_from_text, is_industry_query
from app.chatbot.utils.keywords import normalize_keyword_list
from app.chatbot.utils.location import infer_admin_areas_from_text, infer_regions_from_text
from app.chatbot.utils.stack_detect import extract_stack_candidates
from app.core.config import settings
from app.core.redis import get_redis

logger = logging.getLogger(__name__)
KST = ZoneInfo("Asia/Seoul")

_MONTH_RE = re.compile(r"(?:(\d{4})\s*년\s*)?(\d{1,2})\s*월(?:\s*(\d{1,2})\s*일)?")
_THIS_MONTH = ("이번달", "이번 달")
_LAST_MONTH = ("지난달", "지난 달", "저번달", "저번 달")
_NEXT_MONTH = ("다음달", "다음 달")
_FROM_DATE_HINT_RE = re.compile(r"(부터|이후|뒤로|이후로|이후부터|지금까지|현재까지|까지)")
_STRICT_AFTER_HINT_RE = re.compile(r"(후에|후로|후|뒤)(?=\s|$|[가-힣])")

_TODAY_WORDS = ("오늘",)
_YESTERDAY_WORDS = ("어제",)
_TOMORROW_WORDS = ("내일",)
_DAY_BEFORE_YESTERDAY_WORDS = ("그저께", "그제")

_THIS_WEEK = ("이번주", "이번 주")
_LAST_WEEK = ("지난주", "지난 주", "저번주", "저번 주")
_NEXT_WEEK = ("다음주", "다음 주")

_KOR_NUM = {
    "한": 1, "두": 2, "세": 3, "네": 4,
    "다섯": 5, "여섯": 6, "일곱": 7, "여덟": 8, "아홉": 9, "열": 10,
}
_RECENT_RE = re.compile(r"(최근|요즘)\s*([0-9]{1,3}|한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)?\s*(일|주|주일|달|개월|년)")
_BEFORE_RE = re.compile(r"([0-9]{1,3}|한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)\s*(일|주|주일|달|개월|년)\s*전")

# -------------------------
# Salary parsing (만원 단위)
# -------------------------
_SALARY_HINT_RE = re.compile(r"(연봉|급여|월급|보수)")
_EOK_RE = re.compile(r"(\d+)\s*억(?:\s*(\d+)\s*천)?")          # 1억, 1억2천
_THOUSAND_RE = re.compile(r"(\d+)\s*천\s*(?:만|만원)?")       # 8천(만원)
_NUM_RE = re.compile(r"(\d[\d,]*)\s*(?:만|만원|원)?")         # 8000, 8,000, 8000만원, 80,000,000원
_AMT_TOKEN_RE = re.compile(
    r"(\d+\s*억(?:\s*\d+\s*천)?|\d+\s*천\s*(?:만|만원)?|\d[\d,]*\s*(?:만|만원|원)?)"
)

def _iter_salary_amount_tokens(sub: str) -> List[str]:
    """
    _AMT_TOKEN_RE 매칭 결과 중, 바로 뒤 글자가 '월/일/년'인 경우(날짜)는 버린다.
    예) '1월', '17일', '2026년' 같은 토큰이 연봉으로 오인되는 버그 방지
    """
    out: List[str] = []
    if not sub:
        return out

    for m in _AMT_TOKEN_RE.finditer(sub.replace(" ", "")):
        tok = m.group(1)
        if not tok:
            continue
        # 매칭 직후 문자를 확인해서 날짜 단위면 스킵
        end = m.end()
        after = sub[end:end+1]  # 원문 기준 한 글자
        if after in ("월", "일", "년"):
            continue
        out.append(tok)
    return out


# -------------------------
# Rate parsing (%)
# - 여기서 "지원률/경쟁률"은 공고별 경쟁률(%) = apply_count/recruitment_capacity*100 기준 필터로 해석
# -------------------------
_RATE_TARGET_RE = re.compile(r"(지원률|경쟁률)")
_RATE_CMP_RE = re.compile(
    r"(지원률|경쟁률)\s*(?:이|가)?\s*([0-9]{1,4}(?:\.[0-9]+)?)\s*(?:%|퍼|percent)?\s*(이하|미만|이상|초과|<=|>=|<|>)"
)
_RATE_BETWEEN_RE = re.compile(
    r"(지원률|경쟁률)\s*([0-9]{1,4}(?:\.[0-9]+)?)\s*(?:~|-|–|—|부터|에서)\s*([0-9]{1,4}(?:\.[0-9]+)?)\s*(?:%|퍼|percent)?"
)

# -------------------------
# Experience parsing (years, integer)
# - job_posting.required_experience (int)
# -------------------------
_EXP_HINT_RE = re.compile(r"(경력|경험|연차|년차)")
_EXP_UNBOUNDED_RE = re.compile(r"(경력무관|무관|신입/경력|신입\/경력|경력\/신입)")
_EXP_RANGE_RE = re.compile(r"([0-9]{1,2})\s*년?\s*(?:~|-|–|—)\s*([0-9]{1,2})\s*년?")  # 3~5, 3~5년, 3년~5년
_EXP_CMP_RE = re.compile(r"([0-9]{1,2})\s*년?\s*(이상|초과|이하|미만|<=|>=|<|>)")     # 3년 이상
_EXP_EXACT_RE = re.compile(r"([0-9]{1,2})\s*년\s*(정도|내외|가량)?")                  # 2년 정도
_EXP_YEARCHA_RE = re.compile(r"([0-9]{1,2})\s*년차")                                  # 3년차

# 요구사항 매핑(그대로)
_EXP_WORD_MAP = [
    ("신입", 0, 0),
    ("주니어", 1, 3),
    ("시니어", 3, 5),
    ("미들", 6, None),
    ("미드", 6, None),
    ("중급", 6, None),
]


def _infer_rate_filter_pct_from_text(msg: str) -> dict:
    out = {"min_competition_pct": None, "max_competition_pct": None}
    if not msg:
        return out

    m2 = _RATE_BETWEEN_RE.search(msg)
    if m2:
        a = float(m2.group(2))
        b = float(m2.group(3))
        lo, hi = (a, b) if a <= b else (b, a)
        out["min_competition_pct"] = lo
        out["max_competition_pct"] = hi
        return out

    m = _RATE_CMP_RE.search(msg)
    if not m:
        return out

    val = float(m.group(2))
    op = m.group(3)

    if op in ("이상", ">="):
        out["min_competition_pct"] = val
    elif op in ("초과", ">"):
        out["min_competition_pct"] = val + 1e-9
    elif op in ("이하", "<="):
        out["max_competition_pct"] = val
    elif op in ("미만", "<"):
        out["max_competition_pct"] = val - 1e-9

    return out


def _infer_required_experience_years_from_text(msg: str) -> dict:
    """
    returns:
      {"min": Optional[int], "max": Optional[int], "source": str|None}

    규칙:
    - '신입' => 0
    - '주니어' => 1~3
    - '시니어' => 3~5   (== required_experience >=3 and <=5)
    - '미들/중급' => 6~
    - '경력 3~5년' => 3~5
    - '경력 3년 이상' => >=3
    - '경험 2년 정도' / '2년차' => ==2
    - 단어 매핑 + 숫자 조건이 같이 있으면 교집합(intersection).
      교집합 불가능하면 숫자 조건 우선.
    - '경력무관', '신입/경력' 등은 필터 없음 처리
    """
    out = {"min": None, "max": None, "source": None}
    if not msg:
        return out

    t = msg.replace(" ", "")
    if _EXP_UNBOUNDED_RE.search(t):
        return out

    base_min, base_max, base_src = None, None, None
    for word, lo, hi in _EXP_WORD_MAP:
        if word in msg:
            base_min, base_max, base_src = lo, hi, f"word:{word}"
            break

    has_hint = bool(_EXP_HINT_RE.search(msg)) or (base_src is not None)

    num_min, num_max, num_src = None, None, None
    if has_hint:
        # 0) n년차
        m = _EXP_YEARCHA_RE.search(t)
        if m:
            v = int(m.group(1))
            num_min, num_max, num_src = v, v, "yearcha"
        else:
            # 1) range
            m = _EXP_RANGE_RE.search(t)
            if m:
                a = int(m.group(1))
                b = int(m.group(2))
                lo, hi = (a, b) if a <= b else (b, a)
                num_min, num_max, num_src = lo, hi, "range"
            else:
                # 2) compare
                m = _EXP_CMP_RE.search(t)
                if m:
                    v = int(m.group(1))
                    op = m.group(2)
                    if op in ("이상", ">="):
                        num_min, num_max, num_src = v, None, "cmp:>="
                    elif op in ("초과", ">"):
                        num_min, num_max, num_src = v + 1, None, "cmp:>"
                    elif op in ("이하", "<="):
                        num_min, num_max, num_src = None, v, "cmp:<="
                    elif op in ("미만", "<"):
                        num_min, num_max, num_src = None, v - 1, "cmp:<"
                else:
                    # 3) exact-ish
                    m = _EXP_EXACT_RE.search(t)
                    if m:
                        v = int(m.group(1))
                        num_min, num_max, num_src = v, v, "exact"

    def _intersect(lo1, hi1, lo2, hi2):
        lo = lo1 if lo1 is not None else lo2
        if lo1 is not None and lo2 is not None:
            lo = max(lo1, lo2)
        hi = hi1 if hi1 is not None else hi2
        if hi1 is not None and hi2 is not None:
            hi = min(hi1, hi2)
        return lo, hi

    if base_src is None and num_src is None:
        return out

    if base_src is not None and num_src is None:
        out["min"], out["max"], out["source"] = base_min, base_max, base_src
    elif base_src is None and num_src is not None:
        out["min"], out["max"], out["source"] = num_min, num_max, num_src
    else:
        lo, hi = _intersect(base_min, base_max, num_min, num_max)
        if (lo is not None and hi is not None) and (lo > hi):
            lo, hi = num_min, num_max
            src = f"{num_src}(override)"
        else:
            src = f"{base_src}+{num_src}"
        out["min"], out["max"], out["source"] = lo, hi, src

    if out["min"] is not None:
        out["min"] = max(0, min(60, int(out["min"])))
    if out["max"] is not None:
        out["max"] = max(0, min(60, int(out["max"])))

    if out["min"] is not None and out["max"] is not None and out["min"] > out["max"]:
        out["min"], out["max"] = out["max"], out["min"]

    return out


def _parse_salary_amount_m만원(token: str) -> Optional[int]:
    if not token:
        return None
    t = token.replace(" ", "")

    m = _EOK_RE.search(t)
    if m:
        eok = int(m.group(1))
        thou = int(m.group(2) or 0)
        return eok * 10000 + thou * 1000

    m = _THOUSAND_RE.search(t)
    if m:
        return int(m.group(1)) * 1000

    m = _NUM_RE.search(t)
    if not m:
        return None
    raw = m.group(1).replace(",", "")
    if not raw.isdigit():
        return None
    val = int(raw)

    if ("원" in t) or (val >= 1_000_000):
        return val // 10000

    return val

_SALARY_RANGE_SEP_RE = re.compile(r"(~|∼|〜|–|—|-|부터|에서)")

def _infer_salary_range_m만원_from_text(msg: str) -> Optional[tuple[int, int]]:
    if not msg:
        return None
    m_hint = _SALARY_HINT_RE.search(msg)
    if not m_hint:
        return None

    sub = msg[m_hint.start():]

    # ✅ 범위 표지가 없으면 "범위"로 해석하지 않는다 (오탐 방지 핵심)
    if not _SALARY_RANGE_SEP_RE.search(sub):
        return None

    amts = _iter_salary_amount_tokens(sub)
    if len(amts) < 2:
        return None

    a = _parse_salary_amount_m만원(amts[0])
    b = _parse_salary_amount_m만원(amts[1])
    if a is None or b is None:
        return None
    lo, hi = (a, b) if a <= b else (b, a)
    return lo, hi



def _infer_min_salary_m만원_from_text(msg: str) -> Optional[int]:
    if not msg:
        return None
    if not _SALARY_HINT_RE.search(msg):
        return None

    m_hint = _SALARY_HINT_RE.search(msg)
    sub = msg[m_hint.start():] if m_hint else msg

    amts = _iter_salary_amount_tokens(sub)
    if not amts:
        return None

    return _parse_salary_amount_m만원(amts[0])


def _num_token_to_int(tok: str, default: int = 1) -> int:
    if not tok:
        return default
    t = tok.strip()
    if t.isdigit():
        return int(t)
    return int(_KOR_NUM.get(t, default))


def _safe_date(y: int, m: int, d: int) -> date:
    last = calendar.monthrange(y, m)[1]
    return date(y, m, min(d, last))


def _start_of_week(d: date) -> date:
    return d - timedelta(days=d.weekday())


def _add_month(y: int, m: int, delta: int) -> tuple[int, int]:
    m2 = m + delta
    y2 = y + (m2 - 1) // 12
    m2 = (m2 - 1) % 12 + 1
    return y2, m2


def _infer_recent_range(t: str, today: date) -> tuple[date | None, date | None]:
    m = _RECENT_RE.search(t)
    if not m:
        return None, None
    n = _num_token_to_int(m.group(2), default=1)
    unit = m.group(3)

    end = today + timedelta(days=1)
    if unit == "일":
        return today - timedelta(days=n), end
    if unit in ("주", "주일"):
        return today - timedelta(days=7 * n), end
    if unit in ("달", "개월"):
        y, mm = _add_month(today.year, today.month, -n)
        s = _safe_date(y, mm, today.day)
        return s, end
    if unit == "년":
        s = _safe_date(today.year - n, today.month, today.day)
        return s, end
    return None, None


def _infer_n_before_range(t: str, today: date) -> tuple[date | None, date | None]:
    m = _BEFORE_RE.search(t)
    if not m:
        return None, None

    n = _num_token_to_int(m.group(1), default=1)
    unit = m.group(2)
    tail = t[m.end():]

    if unit == "일":
        base = today - timedelta(days=n)
    elif unit in ("주", "주일"):
        base = today - timedelta(days=7 * n)
    elif unit in ("달", "개월"):
        y, mm = _add_month(today.year, today.month, -n)
        base = _safe_date(y, mm, today.day)
    elif unit == "년":
        base = _safe_date(today.year - n, today.month, today.day)
    else:
        return None, None

    if _FROM_DATE_HINT_RE.search(tail):
        return base, today + timedelta(days=1)
    return base, base + timedelta(days=1)


def _first_day(y: int, m: int) -> date:
    return date(y, m, 1)


def _today_kst() -> date:
    return datetime.now(tz=KST).date()


def _infer_range_from_text(msg: str) -> tuple[date | None, date | None]:
    if not msg:
        return None, None
    t = msg.strip()
    today = _today_kst()

    if any(k in t for k in _TODAY_WORDS):
        return today, today + timedelta(days=1)
    if any(k in t for k in _YESTERDAY_WORDS):
        d = today - timedelta(days=1)
        return d, d + timedelta(days=1)
    if any(k in t for k in _DAY_BEFORE_YESTERDAY_WORDS):
        d = today - timedelta(days=2)
        return d, d + timedelta(days=1)
    if any(k in t for k in _TOMORROW_WORDS):
        d = today + timedelta(days=1)
        return d, d + timedelta(days=1)

    s, e = _infer_recent_range(t, today)
    if s and e:
        return s, e

    s, e = _infer_n_before_range(t, today)
    if s and e:
        return s, e

    if any(k in t for k in _THIS_WEEK):
        s = _start_of_week(today)
        e = today + timedelta(days=1)
        return s, e
    if any(k in t for k in _LAST_WEEK):
        this_start = _start_of_week(today)
        s = this_start - timedelta(days=7)
        e = this_start
        return s, e
    if any(k in t for k in _NEXT_WEEK):
        this_start = _start_of_week(today)
        s = this_start + timedelta(days=7)
        e = s + timedelta(days=7)
        return s, e

    if any(k in t for k in _THIS_MONTH):
        y, m = today.year, today.month
        s = _first_day(y, m)
        y2, m2 = _add_month(y, m, 1)
        e = _first_day(y2, m2)
        return s, e

    if any(k in t for k in _LAST_MONTH):
        y, m = _add_month(today.year, today.month, -1)
        s = _first_day(y, m)
        y2, m2 = _add_month(y, m, 1)
        e = _first_day(y2, m2)
        return s, e

    if any(k in t for k in _NEXT_MONTH):
        y, m = _add_month(today.year, today.month, 1)
        s = _first_day(y, m)
        y2, m2 = _add_month(y, m, 1)
        e = _first_day(y2, m2)
        return s, e

    m = _MONTH_RE.search(t)
    if not m:
        return None, None

    year = int(m.group(1) or today.year)
    month = int(m.group(2))
    day = m.group(3)
    tail = t[m.end():]

    if day:
        s = date(year, month, int(day))

        if _STRICT_AFTER_HINT_RE.search(tail):
            s = s + timedelta(days=1)
            e = today + timedelta(days=1)
            return s, e

        if _FROM_DATE_HINT_RE.search(tail):
            e = today + timedelta(days=1)
            return s, e

        e = s + timedelta(days=1)
        return s, e

    s = date(year, month, 1)

    if _FROM_DATE_HINT_RE.search(tail):
        e = today + timedelta(days=1)
        return s, e

    y2, m2 = _add_month(year, month, 1)
    e = date(y2, m2, 1)
    return s, e


def _default_range(_: ChatbotIntent) -> tuple[date, date]:
    today = _today_kst()
    lookback = int(getattr(settings, "CHATBOT_DEFAULT_LOOKBACK_DAYS", 30))
    return today - timedelta(days=lookback), today + timedelta(days=1)


def _clamp_range(start: date, end: date) -> tuple[date, date]:
    if end <= start:
        end = start + timedelta(days=1)
    max_days = int(getattr(settings, "CHATBOT_MAX_RANGE_DAYS", 365))
    if (end - start).days > max_days:
        end = start + timedelta(days=max_days)
    return start, end


def _clamp_limit(n: Optional[int], default: int = 5) -> int:
    try:
        x = int(n) if n is not None else default
    except Exception:
        x = default
    return max(1, min(20, x))


def _infer_intent_by_rule(msg: str) -> Optional[ChatbotIntent]:
    if not msg:
        return None
    ml = msg.lower()

    if _RATE_TARGET_RE.search(msg):
        if any(k in msg for k in ("몇개", "몇 개", "몇건", "몇 건", "건수", "공고 수", "공고수")):
            return ChatbotIntent.COUNT_POSTINGS
        return ChatbotIntent.RATE_STATS

    if any(k in msg for k in ("보여줘", "목록", "리스트", "최신", "랜덤")):
        return ChatbotIntent.LIST_POSTINGS

    if any(k in msg for k in ("몇개", "몇 개", "몇건", "몇 건", "건수", "공고 수", "공고수")):
        return ChatbotIntent.COUNT_POSTINGS

    if ("스택" in msg) and any(k in msg for k in ("많이", "상위", "제일", "top")):
        return ChatbotIntent.TOP_STACKS

    if "경쟁률" in msg:
        return ChatbotIntent.COMPETITION

    return None


def _coerce_last(last_parsed_dict: Dict[str, Any]) -> Optional[ChatbotParsedSpec]:
    try:
        return ChatbotParsedSpec.model_validate(last_parsed_dict or {})
    except Exception:
        return None


def _display_stack(token: str) -> str:
    t = (token or "").strip()
    tl = t.lower()

    if tl == "java":
        return "JAVA"
    if tl == "python":
        return "Python"
    if tl == "javascript":
        return "JavaScript"
    if tl == "typescript":
        return "TypeScript"

    if " " in tl:
        return " ".join(w[:1].upper() + w[1:] if w else w for w in tl.split(" "))

    return tl[:1].upper() + tl[1:] if tl else t


def _pick_stack_typo_note(stack_corrections: List[dict]) -> Optional[str]:
    for c in (stack_corrections or []):
        via = str(c.get("via") or "")
        if not via.startswith("vocab_trgm"):
            continue
        frm = c.get("from")
        to = c.get("to")
        if not frm or not to:
            continue
        sim = c.get("sim")
        sim_txt = ""
        if isinstance(sim, (float, int)):
            sim_txt = f" (유사도 {float(sim):.2f})"
        return (
            f"입력한 기술스택 '{frm}'은(는) '{_display_stack(str(to))}'로 해석했습니다{sim_txt}.\n"
            f"만약 다른 기술스택을 찾는 거라면 기술스택을 다시 입력해주세요."
        )
    return None


class ChatbotState(TypedDict, total=False):
    request_id: str
    conversation_id: str
    conversation_mode: str
    turn: int

    message: str
    parsed: ChatbotParsedSpec
    result: Dict[str, Any]
    answer: str

    last_parsed_dict: Dict[str, Any]
    last_intent: str
    last_item_ids: List[int]

    scope_job_ids: List[int]


_FOLLOWUP_MARKERS = ("그 공고", "그공고", "그 중", "그중", "그 중에", "그중에", "그러면", "그럼", "거기서", "방금", "이전")


def _is_followup(msg: str) -> bool:
    return bool(msg) and any(m in msg for m in _FOLLOWUP_MARKERS)


class JobStatsChatbot:
    def __init__(self):
        chatbot_model = getattr(settings, "CHATBOT_OPENAI_MODEL_NAME", None) or settings.OPENAI_MODEL_NAME
        chatbot_temp = float(getattr(settings, "CHATBOT_OPENAI_TEMPERATURE", 0.0))

        self.llm = ChatOpenAI(
            model=chatbot_model,
            temperature=chatbot_temp,
            openai_api_key=settings.OPENAI_API_KEY,
        )

        self.parser = PydanticOutputParser(pydantic_object=ChatbotParsedSpec)
        self.graph = self._build_graph()

        ttl = int(getattr(settings, "CHATBOT_CONVERSATION_TTL_SECONDS", 604800))
        self.memory = RedisChatbotMemoryStore(get_redis(), ttl_seconds=ttl)

        self.max_full_turns = int(getattr(settings, "CHATBOT_MAX_FULL_TURNS", 10))
        self.max_transcript_items = int(getattr(settings, "CHATBOT_MAX_TRANSCRIPT_ITEMS", 200))

    def _build_graph(self):
        sg = StateGraph(ChatbotState)
        sg.add_node("parse", self._parse)
        sg.add_node("validate", self._validate)
        sg.add_node("execute", self._execute)
        sg.add_node("answer", self._answer)

        sg.set_entry_point("parse")
        sg.add_edge("parse", "validate")
        sg.add_edge("validate", "execute")
        sg.add_edge("execute", "answer")
        sg.add_edge("answer", END)
        return sg.compile()

    async def _parse(self, state: ChatbotState) -> Dict[str, Any]:
        msg = state.get("message", "")
        today_d = _today_kst()
        today = today_d.isoformat()
        year = today_d.year

        system = """[Role]
너는 취업 공고 통계용 챗봇의 '질문 파서(parser)'다.
출력은 서버가 DB 조회 파라미터로만 사용한다.

[Rules]
- 추측 금지.
- intent는 가능하면 반드시 COUNT/LIST/COMPETITION/TOP_STACKS/RATE_STATS 중 하나로 선택하라.
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
- job_role: 자유 문자열(예: Backend, Frontend, Data Engineer 등). 확장 가능.
- limit: 1..20
- random: LIST_POSTINGS에서 랜덤 여부
- 리스트 필드는 비어도 반드시 []로 출력, null 금지

[Canonicalization]
- 기술스택/업종/직군/툴 이름은 가능한 표준 표기로 정규화하라.
  예: "스프링부트", "spring boot" -> "Spring Boot"
      "js" -> "JavaScript"
  확신이 없으면 사용자가 쓴 원문을 그대로 사용.

[Multi-turn]
- 사용자가 '그 중에/그러면/거기서'처럼 후속 질문을 하면,
  [Conversation context]의 이전 parsed spec을 참고해 누락 필드를 채울 수 있다.
  그래도 불확실하면 HELP.

[Supported intents]
1) COUNT_POSTINGS
2) COMPETITION
3) LIST_POSTINGS (최신/랜덤)
4) TOP_STACKS
5) RATE_STATS (지원률/경쟁률 % 통계)
6) HELP
"""

        examples = f"""[Today]
{today}

[Examples]
- '오늘 공고 몇개 올라왔어?' => COUNT_POSTINGS, start_date=today, end_date=today+1
- '올해 8월 공고 몇개' => COUNT_POSTINGS, start_date={year}-08-01, end_date={year}-09-01
- '백엔드 자바/스프링부트 공고 5개 최신' => LIST_POSTINGS, job_role='Backend', keywords_all=['Java','Spring Boot'], limit=5, random=false
- '서울 강남구 연봉 4천 이상 공고 5개 랜덤' => LIST_POSTINGS, regions_any=['SEOUL'], admin_areas_any=['강남구'], min_salary_m만원=4000, limit=5, random=true
- '1월 9일 뒤로 연봉 8천~9천 공고 몇개' => COUNT_POSTINGS, start_date={year}-01-09, end_date=today+1, min=8000, max=9000
- '경기도 요즘 평균 지원률/경쟁률' => RATE_STATS, regions_any=['GYEONGGI']
- '경쟁률 120% 이하 공고 몇개' => COUNT_POSTINGS, max_competition_pct=120
- '신입 개발자 모집하는 공고는 몇개야?' => COUNT_POSTINGS, min_required_experience_years=0, max_required_experience_years=0
- '경력이 3~5년 정도를 뽑는 공고 몇개야?' => COUNT_POSTINGS, min_required_experience_years=3, max_required_experience_years=5
- '산업분야 중 IT 관련 공고는 몇개야?' => COUNT_POSTINGS, industries_any=['it']
- 'AI, 데이터 서비스 업종 공고 몇개야?' => COUNT_POSTINGS, industries_any=['ai','데이터 서비스']
"""

        context_dict = state.get("last_parsed_dict") or {}
        context_json = json.dumps(context_dict, ensure_ascii=False) if context_dict else ""

        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", system + "\n\n{format_instructions}"),
                (
                    "user",
                    examples
                    + "\n\n[Conversation context]\n{context}\n"
                    + "\n[User question]\n{message}",
                ),
            ]
        ).partial(format_instructions=self.parser.get_format_instructions())

        chain = prompt | self.llm | self.parser
        payload = {"message": msg, "context": context_json}

        try:
            parsed: ChatbotParsedSpec = await chain.ainvoke(payload)
            return {"parsed": parsed}
        except Exception:
            logger.exception("chatbot parse failed (1st)")
            raw = await (prompt | self.llm).ainvoke(payload)
            logger.error("LLM raw output: %s", getattr(raw, "content", raw))
            try:
                parsed: ChatbotParsedSpec = await chain.ainvoke(payload)
                return {"parsed": parsed}
            except Exception:
                logger.exception("chatbot parse failed (2nd)")
                return {"parsed": ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)}

    async def _validate(self, state: ChatbotState) -> Dict[str, Any]:
        parsed = state.get("parsed") or ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)
        msg = state.get("message", "") or ""

        last = _coerce_last(state.get("last_parsed_dict") or {})
        is_follow = _is_followup(msg) and (last is not None)

        # 1) follow-up에서 HELP/저신뢰면 intent 구제 (룰 → last.intent)
        if is_follow and (parsed.intent == ChatbotIntent.HELP or parsed.confidence < 0.45):
            fb = _infer_intent_by_rule(msg)
            if fb is not None:
                parsed.intent = fb
                parsed.confidence = max(parsed.confidence, 0.7)
            else:
                try:
                    parsed.intent = last.intent
                    parsed.confidence = max(parsed.confidence, 0.7)
                except Exception:
                    pass

        # 2) 지원률/경쟁률 문장이면 RATE_STATS로 정규화
        if _RATE_TARGET_RE.search(msg):
            if any(k in msg for k in ("몇개", "몇 개", "몇건", "몇 건", "건수", "공고 수", "공고수")):
                parsed.intent = ChatbotIntent.COUNT_POSTINGS
                parsed.confidence = max(parsed.confidence, 0.8)
            else:
                parsed.intent = ChatbotIntent.RATE_STATS
                parsed.confidence = max(parsed.confidence, 0.8)

        # 3) confidence gate (follow-up은 완화)
        thr = 0.45 if not is_follow else 0.25
        if parsed.confidence < thr:
            parsed.intent = ChatbotIntent.HELP

        if parsed.intent == ChatbotIntent.HELP:
            return {"parsed": parsed, "result": {"keyword_corrections": [], "stack_candidates": []}, "scope_job_ids": []}

        # 4) follow-up이면 last_parsed에서 기본 상속
        if is_follow and last is not None:
            parsed.start_date = parsed.start_date or last.start_date
            parsed.end_date = parsed.end_date or last.end_date

            if not parsed.regions_any:
                parsed.regions_any = list(last.regions_any)
            if not parsed.admin_areas_any:
                if re.search(r"([가-힣]{2,10})(구|시|군)\b", msg):
                    parsed.admin_areas_any = infer_admin_areas_from_text(msg, parsed.regions_any)

            if parsed.job_role is None:
                parsed.job_role = last.job_role

            # ✅ 산업 필터 상속
            if not parsed.industries_any:
                parsed.industries_any = list(getattr(last, "industries_any", []) or [])

            if parsed.min_salary_m만원 is None:
                parsed.min_salary_m만원 = last.min_salary_m만원
            if parsed.max_salary_m만원 is None:
                parsed.max_salary_m만원 = last.max_salary_m만원

            if parsed.min_competition_pct is None:
                parsed.min_competition_pct = getattr(last, "min_competition_pct", None)
            if parsed.max_competition_pct is None:
                parsed.max_competition_pct = getattr(last, "max_competition_pct", None)

            # ✅ 경력 필터 상속
            if parsed.min_required_experience_years is None:
                parsed.min_required_experience_years = getattr(last, "min_required_experience_years", None)
            if parsed.max_required_experience_years is None:
                parsed.max_required_experience_years = getattr(last, "max_required_experience_years", None)

            if not parsed.keywords_all:
                parsed.keywords_all = list(last.keywords_all)
            if not parsed.keywords_any:
                parsed.keywords_any = list(last.keywords_any)

        # 5) 날짜: 메시지에 월/일이 있으면 follow-up에서도 override
        s2, e2 = _infer_range_from_text(msg)
        if s2 and e2:
            parsed.start_date, parsed.end_date = s2, e2

        # 6) 그래도 없으면 default_range
        if parsed.start_date is None or parsed.end_date is None:
            s, e = _default_range(parsed.intent)
            parsed.start_date = parsed.start_date or s
            parsed.end_date = parsed.end_date or e
        parsed.start_date, parsed.end_date = _clamp_range(parsed.start_date, parsed.end_date)

        # 7) follow-up에서 스택 후보 추출 (1번만)
        stack_cands: List[str] = []
        if is_follow:
            try:
                stack_cands = extract_stack_candidates(msg) or []
            except Exception:
                logger.exception("extract_stack_candidates failed")
                stack_cands = []

            if stack_cands:
                or_hint = any(x in msg.lower() for x in ("또는", "혹은", " or ", "/", "|"))
                if or_hint:
                    for s in stack_cands:
                        if s not in parsed.keywords_any:
                            parsed.keywords_any.append(s)
                else:
                    for s in stack_cands:
                        if s not in parsed.keywords_all:
                            parsed.keywords_all.append(s)

        # 8) ✅ 산업/업종 힌트가 있으면 industries_any 자동 추론(LLM보다 우선)
        # - 실제 표준화/동의어 매핑은 DB(alias)에서 처리하는 구조라서, 여기서는 "후보 추출"만 함.
        if not parsed.industries_any and is_industry_query(msg):
            parsed.industries_any = infer_industries_from_text(msg)

        # 9) ✅ 연봉 구간 우선 파싱
        sr = _infer_salary_range_m만원_from_text(msg)
        if sr is not None:
            parsed.min_salary_m만원, parsed.max_salary_m만원 = sr
        else:
            smin = _infer_min_salary_m만원_from_text(msg)
            if smin is not None:
                parsed.min_salary_m만원 = smin

        if parsed.min_salary_m만원 is not None and parsed.max_salary_m만원 is not None:
            if parsed.min_salary_m만원 > parsed.max_salary_m만원:
                parsed.min_salary_m만원, parsed.max_salary_m만원 = parsed.max_salary_m만원, parsed.min_salary_m만원

        # 10) 지역/행정구역 룰 추론
        if not parsed.regions_any:
            parsed.regions_any = infer_regions_from_text(msg)

        if (
                not parsed.admin_areas_any
                and parsed.regions_any
                and re.search(r"([가-힣]{2,10})(구|시|군)\b", msg)
        ):
            parsed.admin_areas_any = infer_admin_areas_from_text(msg, parsed.regions_any)

        # 11) 키워드 split/dedupe
        all_norm, corr_all = normalize_keyword_list(parsed.keywords_all, max_items=30)
        any_norm, corr_any = normalize_keyword_list(parsed.keywords_any, max_items=30)
        parsed.keywords_all = all_norm
        parsed.keywords_any = any_norm

        # industries split/dedupe (같은 함수 재사용)
        ind_norm, corr_ind = normalize_keyword_list(parsed.industries_any, max_items=10)
        parsed.industries_any = ind_norm

        # 12) limit
        default_limit = int(getattr(settings, "CHATBOT_DEFAULT_RESULT_LIMIT", 5))
        parsed.limit = _clamp_limit(parsed.limit, default=default_limit)

        # 13) LIST_POSTINGS random default
        if parsed.intent != ChatbotIntent.LIST_POSTINGS:
            parsed.random = None
        elif parsed.random is None:
            ml = msg.lower()
            parsed.random = ("랜덤" in msg) or ("무작위" in msg) or ("random" in ml)

        # 14) ✅ 지원률/경쟁률(%) 필터 파싱(LLM보다 우선)
        rf = _infer_rate_filter_pct_from_text(msg)
        if rf["min_competition_pct"] is not None:
            parsed.min_competition_pct = rf["min_competition_pct"]
        if rf["max_competition_pct"] is not None:
            parsed.max_competition_pct = rf["max_competition_pct"]

        if parsed.min_competition_pct is not None and parsed.max_competition_pct is not None:
            if parsed.min_competition_pct > parsed.max_competition_pct:
                parsed.min_competition_pct, parsed.max_competition_pct = parsed.max_competition_pct, parsed.min_competition_pct

        # ✅ required_experience(년) 파싱(LLM보다 우선)
        er = _infer_required_experience_years_from_text(msg)
        if er["min"] is not None:
            parsed.min_required_experience_years = er["min"]
        if er["max"] is not None:
            parsed.max_required_experience_years = er["max"]

        if (
                parsed.min_required_experience_years is not None
                and parsed.max_required_experience_years is not None
                and parsed.min_required_experience_years > parsed.max_required_experience_years
        ):
            parsed.min_required_experience_years, parsed.max_required_experience_years = (
                parsed.max_required_experience_years,
                parsed.min_required_experience_years,
            )

        # 15) ✅ 후속질의에서 "그 공고들" 스코프(직전 LIST 결과)
        scope_ids: List[int] = []
        if is_follow:
            last_ids = state.get("last_item_ids") or []
            if last_ids and any(x in msg for x in _FOLLOWUP_MARKERS):
                scope_ids = [int(x) for x in last_ids]

        return {
            "parsed": parsed,
            "scope_job_ids": scope_ids,
            "result": {
                "keyword_corrections": (corr_all + corr_any + corr_ind)[:20],
                "stack_candidates": (stack_cands[:20] if is_follow else []),
            },
        }

    async def _execute(self, state: ChatbotState) -> Dict[str, Any]:
        parsed: ChatbotParsedSpec = state["parsed"]
        rid = state.get("request_id") or str(uuid.uuid4())
        base = state.get("result") or {}

        scope_ids = state.get("scope_job_ids") or None

        if parsed.intent == ChatbotIntent.COUNT_POSTINGS:
            r = job_stats_repo.count_postings(
                parsed.start_date,
                parsed.end_date,
                regions_any=parsed.regions_any,
                admin_areas_any=parsed.admin_areas_any,
                keywords_all=parsed.keywords_all,
                keywords_any=parsed.keywords_any,
                industries_any=parsed.industries_any,
                job_role=parsed.job_role,
                min_salary_m만원=parsed.min_salary_m만원,
                max_salary_m만원=parsed.max_salary_m만원,
                min_competition_pct=parsed.min_competition_pct,
                max_competition_pct=parsed.max_competition_pct,
                min_required_experience_years=parsed.min_required_experience_years,
                max_required_experience_years=parsed.max_required_experience_years,
                job_ids_scope=scope_ids,
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == ChatbotIntent.COMPETITION:
            r = job_stats_repo.competition(
                parsed.start_date,
                parsed.end_date,
                regions_any=parsed.regions_any,
                admin_areas_any=parsed.admin_areas_any,
                keywords_all=parsed.keywords_all,
                keywords_any=parsed.keywords_any,
                industries_any=parsed.industries_any,
                job_role=parsed.job_role,
                min_competition_pct=parsed.min_competition_pct,
                max_competition_pct=parsed.max_competition_pct,
                min_required_experience_years=parsed.min_required_experience_years,
                max_required_experience_years=parsed.max_required_experience_years,
                job_ids_scope=scope_ids,
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == ChatbotIntent.RATE_STATS:
            r = job_stats_repo.rate_stats(
                parsed.start_date,
                parsed.end_date,
                regions_any=parsed.regions_any,
                admin_areas_any=parsed.admin_areas_any,
                keywords_all=parsed.keywords_all,
                keywords_any=parsed.keywords_any,
                industries_any=parsed.industries_any,
                job_role=parsed.job_role,
                min_salary_m만원=parsed.min_salary_m만원,
                max_salary_m만원=parsed.max_salary_m만원,
                min_competition_pct=parsed.min_competition_pct,
                max_competition_pct=parsed.max_competition_pct,
                min_required_experience_years=parsed.min_required_experience_years,
                max_required_experience_years=parsed.max_required_experience_years,
                job_ids_scope=scope_ids,
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == ChatbotIntent.LIST_POSTINGS:
            r = job_stats_repo.list_postings(
                parsed.start_date,
                parsed.end_date,
                request_id=rid,
                regions_any=parsed.regions_any,
                admin_areas_any=parsed.admin_areas_any,
                keywords_all=parsed.keywords_all,
                keywords_any=parsed.keywords_any,
                industries_any=parsed.industries_any,
                job_role=parsed.job_role,
                min_salary_m만원=parsed.min_salary_m만원,
                max_salary_m만원=parsed.max_salary_m만원,
                min_competition_pct=parsed.min_competition_pct,
                max_competition_pct=parsed.max_competition_pct,
                min_required_experience_years=parsed.min_required_experience_years,
                max_required_experience_years=parsed.max_required_experience_years,
                limit=int(parsed.limit or 5),
                random=bool(parsed.random),
                job_ids_scope=scope_ids,
            )
            base.update(r)
            return {"result": base}

        if parsed.intent == ChatbotIntent.TOP_STACKS:
            top_default = int(getattr(settings, "CHATBOT_TOP_STACKS_DEFAULT_LIMIT", 10))
            lim = int(parsed.limit or top_default)
            r = job_stats_repo.top_stacks(
                parsed.start_date,
                parsed.end_date,
                regions_any=parsed.regions_any,
                admin_areas_any=parsed.admin_areas_any,
                industries_any=parsed.industries_any,
                job_role=parsed.job_role,
                limit=lim,
            )
            base.update(r)
            return {"result": base}

        return {"result": base}

    async def _answer(self, state: ChatbotState) -> Dict[str, Any]:
        rid = state.get("request_id") or str(uuid.uuid4())
        parsed: ChatbotParsedSpec = state.get("parsed") or ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)
        result = state.get("result") or {}

        if parsed.intent == ChatbotIntent.HELP:
            answer = (
                "지원 예시:\n"
                "- 오늘 공고 몇개 올라왔어?\n"
                "- 서울 강남구 연봉 4천 이상 공고 5개 랜덤\n"
                "- 1월 9일 뒤로 연봉 8천~9천 공고 몇개\n"
                "- 8월 백엔드 자바 최신 5개\n"
                "- 요즘 올라오는 공고에서 제일 많이 요구하는 스택\n"
                "- 경기도 요즘 평균 지원률/경쟁률\n"
                "- 경쟁률 120% 이하 공고 몇개\n"
                "- 신입/주니어/시니어/미들 공고 몇개?\n"
                "- 경력 3~5년 공고 몇개?\n"
                "- 산업분야 중 IT 관련 공고 몇개?\n"
                "- AI, 데이터 서비스 업종 공고 몇개?\n"
            )
            return {"answer": answer, "request_id": rid}

        start = result.get("start_date")
        end = result.get("end_date")

        def filters_summary() -> str:
            parts = []
            if result.get("industries_any"):
                parts.append(f"산업: {', '.join(result['industries_any'])}")
            if result.get("regions_any"):
                parts.append(f"광역: {', '.join(result['regions_any'])}")
            if result.get("admin_areas_any"):
                parts.append(f"세부: {', '.join(result['admin_areas_any'])}")
            if result.get("job_role"):
                parts.append(f"직군: {result['job_role']}")
            if result.get("keywords_all"):
                parts.append(f"AND: {', '.join(result['keywords_all'])}")
            if result.get("keywords_any"):
                parts.append(f"OR: {', '.join(result['keywords_any'])}")

            mn = result.get("min_salary_m만원")
            mx = result.get("max_salary_m만원")
            if mn is not None and mx is not None:
                parts.append(f"연봉범위: {mn}~{mx}만원")
            elif mn is not None:
                parts.append(f"최소연봉: {mn}만원")

            mncp = result.get("min_competition_pct")
            mxcp = result.get("max_competition_pct")
            if mncp is not None and mxcp is not None:
                parts.append(f"경쟁률% 범위: {mncp}~{mxcp}")
            elif mncp is not None:
                parts.append(f"경쟁률% 최소: {mncp}")
            elif mxcp is not None:
                parts.append(f"경쟁률% 최대: {mxcp}")

            mnexp = result.get("min_required_experience_years")
            mxexp = result.get("max_required_experience_years")
            if mnexp is not None and mxexp is not None:
                parts.append(f"경력(년): {mnexp}~{mxexp}")
            elif mnexp is not None:
                parts.append(f"경력(년) 최소: {mnexp}")
            elif mxexp is not None:
                parts.append(f"경력(년) 최대: {mxexp}")

            if result.get("scoped"):
                parts.append(f"스코프: 직전 결과 {result.get('scope_size', 0)}개")

            return (" | ".join(parts)) if parts else "필터: 없음"

        typo_note = _pick_stack_typo_note(result.get("stack_corrections") or [])

        if parsed.intent == ChatbotIntent.COUNT_POSTINGS:
            cnt = int(result.get("count", 0))
            body = f"{start} ~ {end} 공고 수: {cnt}개\n{filters_summary()}"
            answer = (typo_note + "\n" + body) if typo_note else body

        elif parsed.intent == ChatbotIntent.COMPETITION:
            postings = int(result.get("postings", 0))
            applications = int(result.get("applications", 0))
            avg = float(result.get("avg_apply_per_posting", 0.0))
            body = (
                f"{start} ~ {end} 경쟁률(지원수 기반):\n"
                f"- 공고 수: {postings}개\n"
                f"- 총 지원 수: {applications}건\n"
                f"- 공고 1개당 평균 지원 수: {avg:.2f}건\n"
                f"{filters_summary()}"
            )
            answer = (typo_note + "\n" + body) if typo_note else body

        elif parsed.intent == ChatbotIntent.RATE_STATS:
            postings = int(result.get("postings", 0))
            applications = int(result.get("applications", 0))
            cap = int(result.get("total_capacity", 0))
            avg_apply = float(result.get("avg_apply_per_posting", 0.0))
            apply_rate = float(result.get("apply_rate_weighted_pct", 0.0))
            comp_avg = float(result.get("competition_avg_pct", 0.0))

            body = (
                f"{start} ~ {end} 지원/경쟁 통계:\n"
                f"- 공고 수: {postings}개\n"
                f"- 총 지원 수: {applications}건\n"
                f"- 총 모집 인원: {cap}명\n"
                f"- 공고 1개당 평균 지원자 수: {avg_apply:.2f}명\n"
                f"- 지원률(총지원/총모집, 가중): {apply_rate:.2f}%\n"
                f"- 평균 경쟁률(공고별 %, 평균): {comp_avg:.2f}%\n"
                f"{filters_summary()}"
            )
            answer = (typo_note + "\n" + body) if typo_note else body

        elif parsed.intent == ChatbotIntent.LIST_POSTINGS:
            items = result.get("items") or []
            lim = int(result.get("limit", 5))
            rnd = bool(result.get("random"))
            lines = [
                f"{start} ~ {end} 공고 {len(items)}개 (limit={lim}, {'랜덤' if rnd else '최신'}):",
                filters_summary(),
            ]
            for i, it in enumerate(items, 1):
                lines.append(
                    f"{i}. [{it.get('employer_name')}] {it.get('title')} / {it.get('location') or '-'} "
                    f"/ {it.get('salary_text') or '-'} / exp={it.get('required_experience', '-')} "
                    f"/ stack={it.get('stack') or '-'} "
                    f"/ apply={it.get('apply_count', 0)} / cap={it.get('recruitment_capacity', '-')} "
                    f"/ comp%={it.get('competition_pct', '-')} "
                    f"/ created_at={it.get('created_at') or '-'} "
                    f"(job_id={it.get('job_id')})"
                )
            body = "\n".join(lines)
            answer = (typo_note + "\n" + body) if typo_note else body

        elif parsed.intent == ChatbotIntent.TOP_STACKS:
            items = result.get("items") or []
            lim = int(result.get("limit", 10))
            lines = [f"{start} ~ {end} TOP 스택 (상위 {lim}):", filters_summary()]
            for i, it in enumerate(items, 1):
                lines.append(f"{i}. {it['stack']} ({it['count']}회)")
            body = "\n".join(lines)
            answer = (typo_note + "\n" + body) if typo_note else body

        else:
            answer = "지원하지 않는 질문이다."

        return {"answer": answer, "request_id": rid}

    async def ask(self, message: str, request_id: Optional[str] = None, conversation_id: Optional[str] = None) -> Dict[str, Any]:
        rid = request_id or str(uuid.uuid4())
        cid = conversation_id or str(uuid.uuid4())

        ctx = await self.memory.load(cid)
        if ctx is None:
            ctx = ChatbotConversationState(conversation_id=cid)

        ctx.turn += 1
        if ctx.mode == ConversationMode.FULL and ctx.turn > self.max_full_turns:
            ctx.mode = ConversationMode.SUMMARY
            ctx.turn = 1
            ctx.last_item_ids = []

        out = await self.graph.ainvoke(
            {
                "message": message,
                "request_id": rid,
                "conversation_id": cid,
                "conversation_mode": ctx.mode.value,
                "turn": ctx.turn,
                "last_parsed_dict": ctx.last_parsed or {},
                "last_intent": ctx.last_intent or "",
                "last_item_ids": ctx.last_item_ids if ctx.mode == ConversationMode.FULL else [],
            }
        )

        parsed = out.get("parsed") or None
        result = out.get("result") or {}
        answer = out.get("answer", "")

        if parsed is not None and getattr(parsed, "intent", None) != ChatbotIntent.HELP:
            ctx.last_parsed = parsed.model_dump(mode="json")
            ctx.last_intent = parsed.intent.value

            if ctx.mode == ConversationMode.FULL and parsed.intent == ChatbotIntent.LIST_POSTINGS:
                items = result.get("items") or []
                ctx.last_item_ids = [int(it["job_id"]) for it in items if it.get("job_id") is not None]

        now = datetime.now(tz=KST).isoformat()
        ctx.transcript.append(TranscriptItem(role="user", content=message, at=now))
        ctx.transcript.append(TranscriptItem(role="assistant", content=answer[:4000], at=now))
        if len(ctx.transcript) > self.max_transcript_items:
            ctx.transcript = ctx.transcript[-self.max_transcript_items:]

        ctx.updated_at = now
        await self.memory.save(ctx)

        return {
            "request_id": out.get("request_id", rid),
            "conversation_id": cid,
            "turn": ctx.turn,
            "mode": ctx.mode.value,
            "answer": answer,
            "parsed": parsed,
            "result": result,
        }


chatbot_agent = JobStatsChatbot()
