from __future__ import annotations

import re
from typing import List, Optional

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
_SALARY_RANGE_SEP_RE = re.compile(r"(~|∼|〜|–|—|-|부터|에서)")


def _iter_salary_amount_tokens(sub: str) -> List[str]:
    """Extract salary amount tokens, excluding obvious date tokens.

    Bug guard: tokens immediately followed by '월/일/년' are treated as date fragments.
    """
    out: List[str] = []
    if not sub:
        return out

    compact = sub.replace(" ", "")
    for m in _AMT_TOKEN_RE.finditer(compact):
        tok = m.group(1)
        if not tok:
            continue

        end = m.end()
        after = compact[end : end + 1]
        if after in ("월", "일", "년"):
            continue
        out.append(tok)

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

    # "원" or very large numeric -> treat as KRW and convert to 만원
    if ("원" in t) or (val >= 1_000_000):
        return val // 10000

    # otherwise assume 만원 unit
    return val


def infer_salary_range_m만원_from_text(msg: str) -> Optional[tuple[int, int]]:
    if not msg:
        return None
    m_hint = _SALARY_HINT_RE.search(msg)
    if not m_hint:
        return None

    sub = msg[m_hint.start() :]

    # if there is no range marker, do not treat it as a range
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


def infer_min_salary_m만원_from_text(msg: str) -> Optional[int]:
    if not msg:
        return None
    if not _SALARY_HINT_RE.search(msg):
        return None

    m_hint = _SALARY_HINT_RE.search(msg)
    sub = msg[m_hint.start() :] if m_hint else msg

    amts = _iter_salary_amount_tokens(sub)
    if not amts:
        return None

    return _parse_salary_amount_m만원(amts[0])


# -------------------------
# Rate parsing (%)
# - "지원률/경쟁률"은 공고별 경쟁률(%) = apply_count/recruitment_capacity*100 필터로 해석
# -------------------------
_RATE_TARGET_RE = re.compile(r"(지원률|경쟁률)")
_RATE_CMP_RE = re.compile(
    r"(지원률|경쟁률)\s*(?:이|가)?\s*([0-9]{1,4}(?:\.[0-9]+)?)\s*(?:%|퍼|percent)?\s*(이하|미만|이상|초과|<=|>=|<|>)"
)
_RATE_BETWEEN_RE = re.compile(
    r"(지원률|경쟁률)\s*([0-9]{1,4}(?:\.[0-9]+)?)\s*(?:~|-|–|—|부터|에서)\s*([0-9]{1,4}(?:\.[0-9]+)?)\s*(?:%|퍼|percent)?"
)


def has_rate_target(msg: str) -> bool:
    return bool(msg) and bool(_RATE_TARGET_RE.search(msg))


def infer_rate_filter_pct_from_text(msg: str) -> dict:
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


# -------------------------
# Experience parsing (years, integer)
# - job_posting.required_experience (int)
# -------------------------
_EXP_HINT_RE = re.compile(r"(경력|경험|연차|년차)")
_EXP_UNBOUNDED_RE = re.compile(r"(경력무관|무관|신입/경력|신입\/경력|경력\/신입)")
_EXP_RANGE_RE = re.compile(r"([0-9]{1,2})\s*년?\s*(?:~|-|–|—)\s*([0-9]{1,2})\s*년?")
_EXP_CMP_RE = re.compile(r"([0-9]{1,2})\s*년?\s*(이상|초과|이하|미만|<=|>=|<|>)")
_EXP_EXACT_RE = re.compile(r"([0-9]{1,2})\s*년\s*(정도|내외|가량)?")
_EXP_YEARCHA_RE = re.compile(r"([0-9]{1,2})\s*년차")

_EXP_WORD_MAP = [
    ("신입", 0, 0),
    ("주니어", 1, 3),
    ("시니어", 3, 5),
    ("미들", 6, None),
    ("미드", 6, None),
    ("중급", 6, None),
]


def infer_required_experience_years_from_text(msg: str) -> dict:
    """Infer required experience filter.

    Returns:
      {"min": Optional[int], "max": Optional[int], "source": Optional[str]}

    Intersection rules follow the original implementation.
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
        m = _EXP_YEARCHA_RE.search(t)
        if m:
            v = int(m.group(1))
            num_min, num_max, num_src = v, v, "yearcha"
        else:
            m = _EXP_RANGE_RE.search(t)
            if m:
                a = int(m.group(1))
                b = int(m.group(2))
                lo, hi = (a, b) if a <= b else (b, a)
                num_min, num_max, num_src = lo, hi, "range"
            else:
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
