from langchain_openai import OpenAIEmbeddings
from app.core.config import settings

class VectorService:
    def __init__(self):
        try:
            self.embeddings = OpenAIEmbeddings(
                model=settings.OPENAI_EMBEDDING_MODEL,
                openai_api_key=settings.OPENAI_API_KEY
            )
        except Exception:
            # Fallback for old access tokens
            print("Warning: Access denied for specific model, falling back to ada-002")
            self.embeddings = OpenAIEmbeddings(
                model="text-embedding-ada-002",
                openai_api_key=settings.OPENAI_API_KEY
            )

    async def generate_vector(self, text: str) -> list[float]:
        """
        텍스트를 임베딩 벡터로 변환합니다.
        """
        return await self.embeddings.aembed_query(text)

vector_service = VectorService()
