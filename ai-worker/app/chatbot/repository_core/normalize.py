# app/chatbot/repository_core/normalize.py
from __future__ import annotations

import re
from typing import Dict, List

# -----------------------------
# Keyword normalization
# -----------------------------

_SPACE_RE = re.compile(r"\s+")
_SEP_RE = re.compile(r"\s*[,/|]\s*")

def _normalize_token(s: str, max_len: int = 50) -> str:
    s = (s or "").strip().lower()
    s = _SPACE_RE.sub(" ", s)
    if len(s) > max_len:
        s = s[:max_len]
    return s

def _normalize_kw_list(xs: List[str], max_items: int = 30, max_len: int = 50) -> List[str]:
    out: List[str] = []
    for x in xs or []:
        s = _normalize_token(x, max_len=max_len)
        if not s:
            continue
        if s not in out:
            out.append(s)
        if len(out) >= max_items:
            break
    return out

# -----------------------------
# Canonical group expansion
# -----------------------------

CANONICAL_GROUP_EXPAND: Dict[str, List[str]] = {
    # 필요 시 확장
    "javascript": ["javascript", "nodejs", "vuejs", "nextjs"],
}

def _canon_members_for_query(canon: str) -> List[str]:
    c = _normalize_token(canon)
    if not c:
        return []
    members = CANONICAL_GROUP_EXPAND.get(c)
    if not members:
        return [c]
    out: List[str] = []
    for m in members:
        nm = _normalize_token(m)
        if nm and nm not in out:
            out.append(nm)
    if c not in out:
        out.insert(0, c)
    return out

__all__ = [
    "_SPACE_RE",
    "_SEP_RE",
    "CANONICAL_GROUP_EXPAND",
    "_normalize_token",
    "_normalize_kw_list",
    "_canon_members_for_query",
]
