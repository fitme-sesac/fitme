from fastapi import FastAPI
from app.core.config import settings
from app.resumes.api.controller import router as resume_router

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
