# app/employer_chatbot/config.py
from pydantic_settings import BaseSettings, SettingsConfigDict


class EmployerChatbotSettings(BaseSettings):
    # 기업용 챗봇 전용 설정
    EMPLOYER_CHATBOT_DEFAULT_LOOKBACK_DAYS: int = 30
    EMPLOYER_CHATBOT_MAX_RANGE_DAYS: int = 370
    EMPLOYER_CHATBOT_DEFAULT_RESULT_LIMIT: int = 10

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        env_prefix="EMPLOYER_CHATBOT_",
    )


employer_chatbot_settings = EmployerChatbotSettings()
