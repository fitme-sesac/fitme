# app/employer_chatbot/services/__init__.py
"""
기업용 채용 AI 챗봇 서비스 모듈

구조:
- agent.py: 메인 챗봇 에이전트 (LangGraph 상태 머신)
- prompts.py: LLM 프롬프트 및 파싱 로직
- date_range.py: 날짜 범위 추론 유틸리티
- render.py: 응답 렌더링
- memory.py: 대화 컨텍스트 메모리
"""

from app.employer_chatbot.services.agent import (
    EmployerStatsChatbot,
    employer_chatbot_agent,
)

__all__ = [
    "EmployerStatsChatbot",
    "employer_chatbot_agent",
]
