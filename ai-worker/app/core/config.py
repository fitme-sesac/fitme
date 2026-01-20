import os
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    # App
    APP_TITLE: str = "AI Worker Service"
    APP_VERSION: str = "1.0.0"
    
    # OpenAI
    OPENAI_API_KEY: str
    OPENAI_MODEL_NAME: str = "gpt-5-mini"
    OPENAI_EMBEDDING_MODEL: str = "text-embedding-3-small"
    
    # Database
    DB_HOST: str
    DB_PORT: str
    DB_NAME: str
    DB_USER: str
    DB_PASSWORD: str

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()
