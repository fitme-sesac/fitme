from __future__ import annotations

import uuid

from fastapi import APIRouter, HTTPException

from app.chatbot.schemas import ChatbotQueryRequest, ChatbotQueryResponse
from app.chatbot.services.agent import chatbot_agent

router = APIRouter(prefix="/chatbot", tags=["chatbot"])


@router.post("/query", response_model=ChatbotQueryResponse)
async def chatbot_query(request: ChatbotQueryRequest):
    """
    - request.conversation_id가 없으면 서버에서 새로 생성
    - agent에 conversation_id 전달(대화 메모리/컨텍스트 유지)
    - response_model(ChatbotQueryResponse) 필수 필드(conversation_id 포함) 항상 채움
    """
    try:
        rid = str(uuid.uuid4())
        conv_id = request.conversation_id or str(uuid.uuid4())

        out = await chatbot_agent.ask(
            request.message,
            request_id=rid,
            conversation_id=conv_id,
        )

        parsed = out.get("parsed")

        return ChatbotQueryResponse(
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
