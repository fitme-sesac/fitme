# app/chatbot/config.py
from pydantic_settings import BaseSettings, SettingsConfigDict

class ChatbotSettings(BaseSettings):
    # 챗봇 전용
    CHATBOT_DEFAULT_LOOKBACK_DAYS: int = 30
    CHATBOT_MAX_RANGE_DAYS: int = 370
    CHATBOT_DEFAULT_RESULT_LIMIT: int = 5
    CHATBOT_TOP_STACKS_DEFAULT_LIMIT: int = 10

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        env_prefix="CHATBOT_",  # 이 경우 env 키는 DEFAULT_LOOKBACK_DAYS 처럼 prefix 뒤를 씀
    )

chatbot_settings = ChatbotSettings()
