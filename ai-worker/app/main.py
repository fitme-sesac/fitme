from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.resumes.api.controller import router as resume_router
from app.jobpostings.api.controller import router as job_router
from app.chatbot.router import router as chatbot_router
from app.employer_chatbot.router import router as employer_chatbot_router
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

# CORS (frontend에서 직접 호출 시 필요)
origins = [o.strip() for o in getattr(settings, "CORS_ORIGINS", "").split(",") if o.strip()]
if not origins:
    origins = ["http://localhost:5173", "http://127.0.0.1:5173", "http://localhost", "http://127.0.0.1"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# [Debug Handler] 422 에러 상세 로깅
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    logging.error(f"[Validation Error] URL: {request.url}")
    logging.error(f"[Validation Error] Body: {exc.body}")
    logging.error(f"[Validation Error] Details: {exc.errors()}")
    return JSONResponse(
        status_code=422,
        content={"detail": exc.errors(), "body": exc.body},
    )


# Health Check
@app.get("/")
def health_check():
    return {"status": "ok", "service": "ai-worker", "version": settings.APP_VERSION}

# Register Routers
app.include_router(resume_router)
app.include_router(chatbot_router)
app.include_router(employer_chatbot_router)
app.include_router(job_router)
