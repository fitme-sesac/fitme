import os
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    # App
    APP_TITLE: str = "AI Worker Service"
    APP_VERSION: str = "1.0.0"

    # OpenAI
    # NOTE: 기본값은 "change_me"(로컬/도커 구동용). 실제 호출 시에는 반드시 환경변수로 교체.
    OPENAI_API_KEY: str = "change_me"
    OPENAI_MODEL_NAME: str = "gpt-5-mini"
    OPENAI_EMBEDDING_MODEL: str = "text-embedding-3-small"

    # 챗봇 전용
    CHATBOT_OPENAI_MODEL_NAME: str = "gpt-4o-mini"
    CHATBOT_OPENAI_TEMPERATURE: float = 0.0

    # CORS
    CORS_ORIGINS: str = "http://localhost:5173,http://127.0.0.1:5173,http://localhost,http://127.0.0.1"

    # Database
    DB_HOST: str = "localhost"
    DB_PORT: str = "5432"
    DB_NAME: str = "fitme_project"
    DB_USER: str = "postgres"
    DB_PASSWORD: str = "change_me"

    # Redis
    REDIS_HOST: str
    REDIS_PORT: int
    REDIS_PASSWORD: str

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()
