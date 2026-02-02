# app/employer_chatbot/router.py
from __future__ import annotations

import uuid

from fastapi import APIRouter, HTTPException

from app.employer_chatbot.schemas import EmployerChatbotQueryRequest, EmployerChatbotQueryResponse
from app.employer_chatbot.services.agent import employer_chatbot_agent

router = APIRouter(prefix="/employer-chatbot", tags=["employer-chatbot"])


@router.post("/query", response_model=EmployerChatbotQueryResponse)
async def employer_chatbot_query(request: EmployerChatbotQueryRequest):
    """
    기업용 챗봇 API
    - request.conversation_id가 없으면 서버에서 새로 생성
    - request.employer_id: 기업 ID (인증 시 전달)
    - agent에 conversation_id 전달(대화 메모리/컨텍스트 유지)
    """
    try:
        rid = str(uuid.uuid4())
        conv_id = request.conversation_id or str(uuid.uuid4())

        out = await employer_chatbot_agent.ask(
            request.message,
            request_id=rid,
            conversation_id=conv_id,
            employer_id=request.employer_id,
        )

        parsed = out.get("parsed")

        return EmployerChatbotQueryResponse(
            request_id=out.get("request_id", rid),
            conversation_id=out.get("conversation_id", conv_id),
            answer=out.get("answer", ""),
            intent=getattr(parsed, "intent", "HELP"),
            data=out.get("result", {}),
            turn=int(out.get("turn", 1)),
            mode=str(out.get("mode", "FULL")),
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
