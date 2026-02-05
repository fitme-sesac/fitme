"""Chatbot agent facade.

This module keeps the public import path stable:
    from app.chatbot.services.agent import chatbot_agent

Implementation details live in app.chatbot.services.agent_core.*
"""

from __future__ import annotations

from app.chatbot.services.agent_core.chatbot import JobStatsChatbot


chatbot_agent = JobStatsChatbot()

__all__ = ["JobStatsChatbot", "chatbot_agent"]
