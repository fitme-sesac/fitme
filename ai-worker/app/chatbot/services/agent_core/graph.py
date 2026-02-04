from __future__ import annotations

from typing import Any

from langchain_core.output_parsers import PydanticOutputParser
from langgraph.graph import END, StateGraph

from app.chatbot.schemas import ChatbotParsedSpec
from app.chatbot.services.agent_core.execute import execute_state
from app.chatbot.services.agent_core.intent import is_followup
from app.chatbot.services.agent_core.llm_parse import parse_message
from app.chatbot.services.agent_core.url_intent import is_detail_url_request
from app.chatbot.services.agent_core.render import render_state
from app.chatbot.services.agent_core.types import ChatbotState
from app.chatbot.services.agent_core.validate import validate_state


def build_graph(*, llm: Any, parser: PydanticOutputParser, repo: Any):
    async def _parse(state: ChatbotState):
        message = state.get("message") or ""

        # ✅ 상세 페이지 URL 요청은 LLM 파싱을 건너뛰고, 직전 결과(job_id)로 링크를 만든다.
        if is_detail_url_request(message.strip()):
            from app.chatbot.schemas import ChatbotParsedSpec, ChatbotIntent

            return {"parsed": ChatbotParsedSpec(intent=ChatbotIntent.DETAIL_URLS, confidence=1.0)}

        last_dict = state.get("last_parsed_dict") or {}

        # ✅ 새 질문(필터/컨텍스트 상속 X)인 경우, LLM에게 context를 아예 주지 않는다.
        # - validate 단계에서의 상속 여부와 무관하게, LLM이 context를 보고 필드를 채워버리는 문제를 차단.
        ctx_for_llm = last_dict if (is_followup(message.strip()) and bool(last_dict)) else {}

        parsed: ChatbotParsedSpec = await parse_message(llm, parser, message=message, last_parsed_dict=ctx_for_llm)
        return {"parsed": parsed}

    async def _validate(state: ChatbotState):
        return await validate_state(state)

    async def _execute(state: ChatbotState):
        return await execute_state(state, repo=repo)

    async def _answer(state: ChatbotState):
        return await render_state(state)

    graph = StateGraph(ChatbotState)
    graph.add_node("parse", _parse)
    graph.add_node("validate", _validate)
    graph.add_node("execute", _execute)
    graph.add_node("answer", _answer)

    graph.set_entry_point("parse")
    graph.add_edge("parse", "validate")
    graph.add_edge("validate", "execute")
    graph.add_edge("execute", "answer")
    graph.add_edge("answer", END)

    return graph.compile()
