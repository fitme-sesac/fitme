from __future__ import annotations

import calendar
import re
from datetime import date, datetime, timedelta
from zoneinfo import ZoneInfo

from app.chatbot.schemas import ChatbotIntent
from app.core.config import settings

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
    "한": 1,
    "두": 2,
    "세": 3,
    "네": 4,
    "다섯": 5,
    "여섯": 6,
    "일곱": 7,
    "여덟": 8,
    "아홉": 9,
    "열": 10,
}

_RECENT_RE = re.compile(r"(최근|요즘)\s*([0-9]{1,3}|한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)?\s*(일|주|주일|달|개월|년)")
_BEFORE_RE = re.compile(r"([0-9]{1,3}|한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)\s*(일|주|주일|달|개월|년)\s*전")


def today_kst() -> date:
    return datetime.now(tz=KST).date()


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
    tail = t[m.end() :]

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


def infer_range_from_text(msg: str) -> tuple[date | None, date | None]:
    """Infer (start_date, end_date_exclusive) from Korean date expressions."""

    if not msg:
        return None, None

    t = msg.strip()
    today = today_kst()

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
    tail = t[m.end() :]

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


def default_range(_: ChatbotIntent) -> tuple[date, date]:
    today = today_kst()
    lookback = int(getattr(settings, "CHATBOT_DEFAULT_LOOKBACK_DAYS", 30))
    return today - timedelta(days=lookback), today + timedelta(days=1)


def clamp_range(start: date, end: date) -> tuple[date, date]:
    if end <= start:
        end = start + timedelta(days=1)
    max_days = int(getattr(settings, "CHATBOT_MAX_RANGE_DAYS", 365))
    if (end - start).days > max_days:
        end = start + timedelta(days=max_days)
    return start, end


def clamp_limit(n: int | None, default: int = 5) -> int:
    try:
        x = int(n) if n is not None else int(default)
    except Exception:
        x = int(default)
    return max(1, min(20, x))
