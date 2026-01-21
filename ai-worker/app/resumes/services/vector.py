import logging
import pydantic
from langchain_openai import OpenAIEmbeddings
from app.core.config import settings

# [안전 장치] openai 패키지가 설치되지 않았을 경우를 대비한 방어적 import
try:
    import openai
except ImportError:
    openai = None

# 모듈 로거 설정 (print 대신 사용)
logger = logging.getLogger(__name__)

from dotenv import load_dotenv
import os
load_dotenv()


try:
    from langsmith import traceable
except ImportError:
    # langsmith가 없는 경우를 대비한 더미 데코레이터
    def traceable(**kwargs):
        def decorator(func):
            return func
        return decorator

class VectorService:
    def __init__(self):
        """
        벡터 서비스 초기화: OpenAI 임베딩 모델을 설정합니다.
        실패 시 구형 모델(ada-002)로 자동 절체(Fallback)하는 로직이 포함되어 있습니다.
        """
        # [Debug] API Key & Tracing Check (Print로 변경)
        tracing = os.getenv("LANGCHAIN_TRACING_V2")
        project = os.getenv("LANGCHAIN_PROJECT")
        api_key_exists = "O" if os.getenv("LANGCHAIN_API_KEY") else "X"
        
        print(f"\n[VectorService Init] Tracing: {tracing}, Project: {project}, API_KEY: {api_key_exists}")
        
        if not settings.OPENAI_API_KEY:
            print("❌ OpenAI API Key is MISSING in settings! Check .env file.")
        else:
            masked = settings.OPENAI_API_KEY[:5] + "..." 
            print(f"[OK] OpenAI API Key loaded. Key: {masked}")

        try:
            # 1. 주력 모델 설정 시도
            self.embeddings = OpenAIEmbeddings(
                model=settings.OPENAI_EMBEDDING_MODEL,
                openai_api_key=settings.OPENAI_API_KEY
            )
            # LangChain은 인스턴스 생성 시점에는 검증을 느슨하게 할 수도 있으나,
            # 여기서는 인스턴스 생성이 성공하면 일단 신뢰합니다.
        except (pydantic.ValidationError, getattr(openai, 'OpenAIError', Exception), Exception) as e:
            # 2. 실패 시 예외 처리: 모델명 오타, 권한 부족, 구형 토큰 등 다양한 원인 대응
            logger.warning(
                f"주력 모델 '{settings.OPENAI_EMBEDDING_MODEL}' 초기화 실패. 백업 모델로 전환합니다.", 
                exc_info=True
            )
            try:
                # 3. 백업 모델(ada-002) 설정
                self.embeddings = OpenAIEmbeddings(
                    model="text-embedding-ada-002",
                    openai_api_key=settings.OPENAI_API_KEY
                )
                # [중요] 백업 모델은 즉시 "테스트 호출"을 날려 검증합니다.
                # 백업조차 안 되면 더 이상 진행할 수 없으므로 Fail-Fast 합니다.
                self.embeddings.embed_query("connection check")
            except Exception as fallback_e:
                logger.error("백업 모델 초기화마저 실패했습니다. 설정을 확인해주세요.", exc_info=True)
                # 상위 호출자에게 에러를 전파하여 서버 실행을 멈춥니다.
                raise fallback_e

    @traceable(name="Generate Embedding Vector", run_type="embedding")
    async def generate_vector(self, text: str) -> list[float]:
        """
        텍스트를 임베딩 벡터로 변환합니다.
        """
        print("\n" + "="*20 + " [Embedding Input Text Check] " + "="*20)
        print(text)
        print("="*66 + "\n")
        return await self.embeddings.aembed_query(text)

vector_service = VectorService()
