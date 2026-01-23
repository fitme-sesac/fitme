from fastapi import FastAPI
from app.core.config import settings
from app.resumes.api.controller import router as resume_router
from app.chatbot.router import router as chatbot_router
import logging
import os
from logging.handlers import RotatingFileHandler

# [로깅 설정]
# 1. 로그 폴더 생성
if not os.path.exists("logs"):
    os.makedirs("logs")

# 2. 로거 기본 설정 (Console + File)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        # 콘솔 출력 (화면)
        logging.StreamHandler(),
        # 파일 저장 (로그가 너무 커지면 자동 교체: 10MB 기준, 최대 5개)
        RotatingFileHandler("logs/server.log", maxBytes=10*1024*1024, backupCount=5, encoding="utf-8")
    ]
)

app = FastAPI(
    title=settings.APP_TITLE,
    version=settings.APP_VERSION
)

# Health Check
@app.get("/")
def health_check():
    return {"status": "ok", "service": "ai-worker", "version": settings.APP_VERSION}

# Register Routers
app.include_router(resume_router)
app.include_router(chatbot_router)
