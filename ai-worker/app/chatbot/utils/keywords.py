from __future__ import annotations

import re
from typing import List, Tuple

SEP_RE = re.compile(r"\s*[,/|]\s*")  # 일반 구분자만 처리


def normalize_keyword_list(xs: List[str], max_items: int = 30, max_len: int = 50) -> Tuple[List[str], List[dict]]:
    """
    Returns (normalized_list, corrections)
    - split by common separators (, / |)
    - trims, de-dupes
    - DOES NOT apply synonym dictionary (LLM handles canonicalization)
    """
    out: List[str] = []
    corrections: List[dict] = []

    for raw in xs or []:
        s0 = (raw or "").strip()
        if not s0:
            continue

        parts = [p.strip() for p in SEP_RE.split(s0) if p.strip()]
        for p in parts:
            s = p
            if len(s) > max_len:
                corrections.append({"from": s, "to": s[:max_len]})
                s = s[:max_len]

            if s not in out:
                out.append(s)
            if len(out) >= max_items:
                break
        if len(out) >= max_items:
            break

    return out, corrections
