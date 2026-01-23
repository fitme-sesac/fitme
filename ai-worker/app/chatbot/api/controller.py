from __future__ import annotations

import uuid

from fastapi import APIRouter, HTTPException

from app.chatbot.schemas import ChatbotQueryRequest, ChatbotQueryResponse
from app.chatbot.services.agent import chatbot_agent


router = APIRouter(prefix="/chatbot", tags=["chatbot"])


@router.post("/query", response_model=ChatbotQueryResponse)
async def chatbot_query(request: ChatbotQueryRequest):
    """Chatbot endpoint.

    Workflow:
    - LLM parses the message into a structured spec
    - server validates + clamps the time range
    - server runs deterministic DB queries
    - server returns a deterministic answer string
    """
    try:
        rid = str(uuid.uuid4())

        # ✅ 대화 이어가기 지원:
        # - request.conversation_id가 있으면 그대로 사용
        # - 없으면 새 conversation_id를 생성
        conv_id = request.conversation_id or str(uuid.uuid4())

        # ✅ agent에 conversation_id 전달 (메모리/컨텍스트 유지)
        out = await chatbot_agent.ask(
            request.message,
            request_id=rid,
            conversation_id=conv_id,
        )

        parsed = out.get("parsed")

        # ✅ response_model(ChatbotQueryResponse) 필수 필드 채워서 반환
        return ChatbotQueryResponse(
            request_id=out.get("request_id", rid),
            conversation_id=out.get("conversation_id", conv_id),
            answer=out.get("answer", ""),
            intent=getattr(parsed, "intent", "HELP"),
            data=out.get("result", {}),
            turn=int(out.get("turn", 1)),
            mode=str(out.get("mode", "chat")),
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
