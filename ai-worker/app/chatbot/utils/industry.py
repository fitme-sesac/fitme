# app/chatbot/utils/industry.py
from __future__ import annotations

import re
from typing import List

# "산업/업종/분야" 질문인지 판단 (스택/기술 질문과 혼동 방지)
_INDUSTRY_HINT_RE = re.compile(r"(산업|업종|분야)")
_INDUSTRY_DISAMBIGUATE_RE = re.compile(r"(스택|기술스택|기술 스택|프레임워크|언어|툴|라이브러리|DB|데이터베이스)")

# 토큰 추출: 영어/숫자 혼합 토큰 + 한글 단어 토큰
_ASCII_TOKEN_RE = re.compile(r"[A-Za-z][A-Za-z0-9\+\#\.\-]{0,30}")
_HANGUL_WORD_RE = re.compile(r"[가-힣]{2,20}")

# 최소 조사 제거
_JOSA_RE = re.compile(r"(은|는|이|가|을|를|에|에서|으로|로|도|만|야|요|죠|까)$")

# 불용어(산업 추론에서 제거)
_STOPWORDS = {
    "산업", "업종", "분야", "관련", "관련한", "관련해", "관한",
    "공고", "채용", "모집", "포지션", "직군", "직무",
    "몇", "몇개", "몇개야", "몇개냐", "몇 개", "몇건", "몇 건",
    "알려줘", "보여줘", "목록", "리스트", "최신", "랜덤", "무작위",
    "오늘", "이번", "이번달", "이번 달", "올해", "요즘", "최근",
    "중", "중에서", "중에", "위주", "위주의",
}

# "IT" 같이 짧은 영문 토큰은 내부 포함 오탐을 막기 위해 boundary 검사
def _has_ascii_token(text_lower: str, token: str) -> bool:
    # 예: "it관련"은 매칭, "fitme" 같은 내부 포함은 매칭 방지
    return re.search(rf"(?i)(^|[^a-z0-9]){re.escape(token)}([^a-z0-9]|$)", text_lower) is not None


def is_industry_query(msg: str) -> bool:
    if not msg:
        return False
    if not _INDUSTRY_HINT_RE.search(msg):
        return False
    # "산업분야"라고 해놓고 실제론 스택 질문일 수 있어서 제외
    if _INDUSTRY_DISAMBIGUATE_RE.search(msg):
        return False
    return True


def infer_industries_from_text(msg: str, max_items: int = 10) -> List[str]:
    """
    하드코딩된 "산업 목록"에 의존하지 않고,
    메시지에서 후보 토큰을 추출해 industries_any(raw)로 넘긴다.
    - 실제 표준화/동의어 매핑은 DB alias(산업 alias 테이블)에서 해결하는 구조를 권장.
    """
    if not msg or not is_industry_query(msg):
        return []

    tl = msg.lower()

    # 1) 구문 후보(예: "데이터 서비스", "ai 서비스")
    # - 특정 산업명을 하드코딩하지 않고 "X 서비스" 같은 형태만 일반화로 잡음
    phrase_out: List[str] = []
    for m in re.finditer(r"([가-힣]{2,15})\s*서비스", msg):
        base = _JOSA_RE.sub("", m.group(1))
        if base and base not in _STOPWORDS:
            phrase_out.append(f"{base} 서비스")

    # 2) 영문 토큰
    ascii_tokens = []
    for m in _ASCII_TOKEN_RE.finditer(msg):
        tok = (m.group(0) or "").strip()
        if not tok:
            continue
        t = tok.lower()

        # it, ai 같은 짧은 토큰은 boundary로 재확인
        if len(t) <= 3:
            if not _has_ascii_token(tl, t):
                continue

        if t in _STOPWORDS:
            continue
        ascii_tokens.append(t)

    # 3) 한글 토큰
    hangul_tokens = []
    for m in _HANGUL_WORD_RE.finditer(msg):
        tok = (m.group(0) or "").strip()
        if not tok:
            continue
        t = _JOSA_RE.sub("", tok)
        if not t or t in _STOPWORDS:
            continue
        hangul_tokens.append(t)

    # 4) 중복 제거 + 우선순위(구문 > 한글 > 영문)
    out: List[str] = []
    def add(x: str):
        x = (x or "").strip()
        if not x:
            return
        if x in _STOPWORDS:
            return
        if x not in out:
            out.append(x)

    for p in phrase_out:
        add(p)
    for h in hangul_tokens:
        add(h)
    for a in ascii_tokens:
        add(a)

    return out[:max_items]
