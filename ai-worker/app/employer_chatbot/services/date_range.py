# app/employer_chatbot/services/date_range.py
"""
날짜 범위 추론 유틸리티
"""
from __future__ import annotations

import calendar
import re
from datetime import date, datetime, timedelta
from typing import Optional, Tuple

from zoneinfo import ZoneInfo

KST = ZoneInfo("Asia/Seoul")


# =============================================================================
# 정규식 패턴
# =============================================================================

_MONTH_RE = re.compile(r"(?:(\d{4})\s*년\s*)?(\d{1,2})\s*월(?:\s*(\d{1,2})\s*일)?")
_RECENT_RE = re.compile(r"(최근|요즘)\s*([0-9]{1,3}|한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)?\s*(일|주|주일|달|개월|년)")
_BEFORE_RE = re.compile(r"([0-9]{1,3}|한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)\s*(일|주|주일|달|개월|년)\s*전")
_FROM_DATE_HINT_RE = re.compile(r"(부터|이후|뒤로|이후로|이후부터|지금까지|현재까지|까지)")
_STRICT_AFTER_HINT_RE = re.compile(r"(후에|후로|후|뒤)(?=\s|$|[가-힣])")

# =============================================================================
# 키워드 상수
# =============================================================================

_TODAY_WORDS = ("오늘",)
_YESTERDAY_WORDS = ("어제",)
_TOMORROW_WORDS = ("내일",)
_DAY_BEFORE_YESTERDAY_WORDS = ("그저께", "그제")

_THIS_WEEK = ("이번주", "이번 주")
_LAST_WEEK = ("지난주", "지난 주", "저번주", "저번 주")
_NEXT_WEEK = ("다음주", "다음 주")

_THIS_MONTH = ("이번달", "이번 달")
_LAST_MONTH = ("지난달", "지난 달", "저번달", "저번 달")
_NEXT_MONTH = ("다음달", "다음 달")

_KOR_NUM = {
    "한": 1, "두": 2, "세": 3, "네": 4,
    "다섯": 5, "여섯": 6, "일곱": 7, "여덟": 8, "아홉": 9, "열": 10,
}


# =============================================================================
# 헬퍼 함수
# =============================================================================

def today_kst() -> date:
    """현재 날짜 (KST 기준)"""
    return datetime.now(tz=KST).date()


def _num_token_to_int(tok: str, default: int = 1) -> int:
    """한글/숫자 토큰을 정수로 변환"""
    if not tok:
        return default
    t = tok.strip()
    if t.isdigit():
        return int(t)
    return int(_KOR_NUM.get(t, default))


def _safe_date(y: int, m: int, d: int) -> date:
    """유효한 날짜로 보정 (말일 처리)"""
    last = calendar.monthrange(y, m)[1]
    return date(y, m, min(d, last))


def _start_of_week(d: date) -> date:
    """주의 시작일 (월요일)"""
    return d - timedelta(days=d.weekday())


def _add_month(y: int, m: int, delta: int) -> Tuple[int, int]:
    """월 덧셈"""
    m2 = m + delta
    y2 = y + (m2 - 1) // 12
    m2 = (m2 - 1) % 12 + 1
    return y2, m2


def _first_day(y: int, m: int) -> date:
    """해당 월의 첫째 날"""
    return date(y, m, 1)


# =============================================================================
# 범위 추론 함수
# =============================================================================

def _infer_recent_range(t: str, today: date) -> Tuple[Optional[date], Optional[date]]:
    """'최근 N일/주/달' 패턴 추론"""
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


def _infer_n_before_range(t: str, today: date) -> Tuple[Optional[date], Optional[date]]:
    """'N일/주/달 전' 패턴 추론"""
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
    
    # '~부터/이후' 포함 시 범위 확장
    if _FROM_DATE_HINT_RE.search(tail):
        return base, today + timedelta(days=1)
    
    return base, base + timedelta(days=1)


def infer_range_from_text(msg: str) -> Tuple[Optional[date], Optional[date]]:
    """
    자연어 텍스트에서 날짜 범위 추론
    
    Returns:
        (start_date, end_date) - end_date는 exclusive
    """
    if not msg:
        return None, None
    
    t = msg.strip()
    today = today_kst()
    
    # 오늘/어제/내일 등 단일 날짜
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
    
    # 최근 N일/주/달
    s, e = _infer_recent_range(t, today)
    if s and e:
        return s, e
    
    # N일/주/달 전
    s, e = _infer_n_before_range(t, today)
    if s and e:
        return s, e
    
    # 이번주/지난주/다음주
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
    
    # 이번달/지난달/다음달
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
    
    # X년 Y월 Z일 형식 파싱
    match = _MONTH_RE.search(t)
    if not match:
        return None, None
    
    year = int(match.group(1) or today.year)
    month = int(match.group(2))
    day = match.group(3)
    tail = t[match.end():]
    
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
    
    # 월만 지정된 경우 해당 월 전체
    s = date(year, month, 1)
    
    if _FROM_DATE_HINT_RE.search(tail):
        e = today + timedelta(days=1)
        return s, e
    
    y2, m2 = _add_month(year, month, 1)
    e = date(y2, m2, 1)
    return s, e


def default_range(lookback_days: int = 30) -> Tuple[date, date]:
    """기본 조회 범위 (최근 N일)"""
    today = today_kst()
    return today - timedelta(days=lookback_days), today + timedelta(days=1)


def clamp_range(start: date, end: date, max_days: int = 365) -> Tuple[date, date]:
    """범위 제한 (최대 N일)"""
    if end <= start:
        end = start + timedelta(days=1)
    if (end - start).days > max_days:
        end = start + timedelta(days=max_days)
    return start, end


def clamp_limit(n: Optional[int], default: int = 10, max_val: int = 50) -> int:
    """결과 개수 제한"""
    try:
        x = int(n) if n is not None else default
    except Exception:
        x = default
    return max(1, min(max_val, x))
