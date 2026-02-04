from __future__ import annotations

import re
from typing import Optional

from app.chatbot.schemas import ChatbotIntent

"""Follow-up detection.

User rule:
- '그러면/그렇다면/그럼/글면(…)' 계열은 기본적으로 **새 질문** (필터/컨텍스트 상속 X)
- 단, 아래 '이어가기 마커'가 포함되면 **후속질의(상속)** 로 처리한다.

Why this matters:
- parse 단계에서 LLM에게 항상 이전 context를 주면, 새 질문에서도 필터가 섞이는 문제가 발생한다.
  (validate에서 상속을 안 하더라도, LLM이 스스로 context를 반영해 필드를 채워버릴 수 있음)
"""


# 기본적으로 '새 질문' 취급(상속 X)하는 접속사/전환어 (문장 맨 앞)
RESET_PREFIXES = (
    "그러면",
    "그러면은",
    "그럼",
    "그렇다면",
    "글면",
)


# '이어가기'로 취급하는 명시 마커 (포함되면 무조건 follow-up)
CONTINUE_MARKERS = (
    # 사용자가 명시한 표현
    "그중에서",
    "그 중에서",
    "거기서",
    "거기에서",
    "이어서",
    "그 공고들중에서",
    "그 공고들 중에서",
    "그공고들중에서",
    "그공고들 중에서",
    # 기존 구현에서 이미 쓰던 표현 (space/붙임 대응)
    "그 공고",
    "그공고",
    "그 공고들",
    "그공고들",
    "그중",
    "그 중",
    "그중에",
    "그 중에",
)


# 기타 follow-up 힌트(기존 유지): 문장 어디에 있어도 follow-up
LEGACY_FOLLOWUP_MARKERS = (
    "방금",
    "이전",
    "아까",
)


_LEADING_PUNCT_RE = re.compile(r"^[\s\t\r\n\.,!?~\-—–\(\)\[\]\{\}\"'“”‘’]+")

_RATE_TARGET_RE = re.compile(r"(지원률|경쟁률)")


def is_followup(msg: str) -> bool:
    if not msg:
        return False

    # 1) 이어가기 마커가 있으면 무조건 follow-up
    if any(m in msg for m in CONTINUE_MARKERS):
        return True

    # 2) 문장 시작이 전환어(그러면/그럼/...)면 새 질문으로 간주
    s = _LEADING_PUNCT_RE.sub("", msg)
    if any(s.startswith(p) for p in RESET_PREFIXES):
        return False

    # 3) 그 외의 기존 힌트
    return any(m in msg for m in LEGACY_FOLLOWUP_MARKERS)


def infer_intent_by_rule(msg: str) -> Optional[ChatbotIntent]:
    """Heuristic intent inference.

    Used mainly to rescue low-confidence LLM parses in follow-up turns.
    """

    if not msg:
        return None

    ml = msg.lower()

    # 0) BUSIEST_WEEK / BUSIEST_DAY (week-first; day is handled in validate for follow-up)
    if any(k in msg for k in ("일주일", "주간", "주 단위", "첫째주", "둘째주", "셋째주", "넷째주", "다섯째주")) or (
        ("주" in msg)
        and ("공고" in msg or "채용" in msg)
        and any(k in msg for k in ("언제", "때", "날"))
    ) and any(k in msg for k in ("언제", "때", "날", "요일")) and any(
        k in msg for k in ("가장", "제일", "최다", "많이", "많은", "많")
    ):
        return ChatbotIntent.BUSIEST_WEEK

    # 1) 경쟁률 극값 공고
    if "경쟁률" in msg and any(k in msg for k in ("낮", "최저", "하위")):
        return ChatbotIntent.LOW_COMPETITION_POSTINGS
    if "경쟁률" in msg and any(k in msg for k in ("높", "최고", "상위")):
        return ChatbotIntent.HIGH_COMPETITION_POSTINGS

    # 2) 스택 지원률 극값
    if ("지원률" in msg) and ("스택" in msg) and any(k in msg for k in ("낮", "최저", "하위")):
        return ChatbotIntent.LOW_STACK_APPLY_RATE
    if ("지원률" in msg) and ("스택" in msg) and any(k in msg for k in ("높", "최고", "상위")):
        return ChatbotIntent.HIGH_STACK_APPLY_RATE

    # 3) 산업 분야 극값
    if any(k in msg for k in ("산업", "업종", "산업분야", "분야")) and any(k in msg for k in ("공고", "공고수", "공고 수")):
        if any(k in msg for k in ("많", "최다", "제일 많", "가장 많")):
            return ChatbotIntent.TOP_INDUSTRIES
        if any(k in msg for k in ("적", "최저", "제일 적", "가장 낮", "낮")):
            return ChatbotIntent.BOTTOM_INDUSTRIES

    # 4) AI 직업군: 지원자 수 최다 공고
    if ("공고" in msg or "채용" in msg) and ("지원" in msg or "지원자" in msg) and any(
        k in msg for k in ("제일", "가장", "최다")
    ) and any(k in ml for k in ("ai", "machine learning", "ml", "머신러닝")):
        return ChatbotIntent.MOST_APPLICANTS_POSTINGS

    # 5) TOP/BOTTOM 스택
    if ("스택" in msg) and any(k in msg for k in ("적게", "적은", "낮은", "최저", "하위", "least", "bottom")):
        return ChatbotIntent.BOTTOM_STACKS

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
