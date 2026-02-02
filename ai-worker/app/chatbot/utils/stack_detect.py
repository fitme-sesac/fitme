from __future__ import annotations

import re
from typing import List, Set

STACK_STOP: Set[str] = {
    "공고","채용","관련","기술","스택","개","건","몇개","몇","최신","랜덤","요즘","오늘","이번","월","올해",
    "연봉","이상","미만","직군","개발","개발자","포지션","조회","보여줘","목록","리스트","경쟁률",
    # ✅ 후속 담화 표지
    "거기서","그러면","그럼","그중","그 중","그중에","그 중에","방금","이전",
}

# "강남구/수원시/기장군" 같은 행정구역 토큰은 스택으로 오인하지 않게 제외
ADMIN_SUFFIX = ("구", "시", "군")

TOKEN_RE = re.compile(r"[A-Za-z][A-Za-z0-9\+\#\.\-]{1,30}|[가-힣]{2,10}")

# ✅ 조사/어미 제거(최소)
_JOSA_RE = re.compile(r"(은|는|이|가|을|를|에|에서|으로|로|도|만|야|요|죠|까)$")

# ✅ 한글 스택 최소 매핑(테이블이 없어도 잡히게)
HANGUL_STACK_MAP = {
    "파이썬": "python",
    "자바": "java",
    "자바스크립트": "javascript",
    "타입스크립트": "typescript",
    "스프링": "spring",
    "스프링부트": "spring boot",
    "장고": "django",
    "플라스크": "flask",
    "리액트": "react",
    "노드": "node.js",
    "노드js": "node.js",
}


def extract_stack_candidates(text: str, max_candidates: int = 8) -> List[str]:
    """
    사용자가 입력한 문장에서 '스택일 가능성이 있는 토큰/구문'을 뽑는다.
    - 영어 토큰은 lower()
    - 한글 토큰은 whitelist/매핑만 통과
    - stopword/행정구역 suffix 제거
    - 'spring boot' 같은 2-gram도 후보로 만든다.
    """
    if not text:
        return []

    raw_tokens = [m.group(0) for m in TOKEN_RE.finditer(text)]
    tokens: List[str] = []

    for t in raw_tokens:
        if t.isascii():
            tl = t.lower()
            if tl in STACK_STOP:
                continue
            tokens.append(tl)
            continue

        # 한글: 조사 제거 후 stop 체크
        base = _JOSA_RE.sub("", t)
        if not base or base in STACK_STOP:
            continue
        if base.endswith(ADMIN_SUFFIX):
            continue

        # 한글 스택은 whitelist/매핑만 통과
        if base in HANGUL_STACK_MAP:
            tokens.append(HANGUL_STACK_MAP[base])

    if not tokens:
        return []

    # 영어 연속 토큰 2-gram: "spring boot"
    candidates: List[str] = []
    for i in range(len(tokens) - 1):
        a, b = tokens[i], tokens[i + 1]
        if a.isascii() and b.isascii():
            candidates.append(f"{a} {b}")

    candidates.extend(tokens)

    out: List[str] = []
    for c in candidates:
        if c and c not in out:
            out.append(c)
        if len(out) >= max_candidates:
            break
    return out
