from __future__ import annotations

from typing import List, Optional


def display_stack(token: str) -> str:
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


def pick_stack_typo_note(stack_corrections: List[dict]) -> Optional[str]:
    """Prefer a single, user-facing typo hint based on trigram correction."""

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
            f"입력한 기술스택 '{frm}'은(는) '{display_stack(str(to))}'로 해석했습니다{sim_txt}.\n"
            f"만약 다른 기술스택을 찾는 거라면 기술스택을 다시 입력해주세요."
        )
    return None
