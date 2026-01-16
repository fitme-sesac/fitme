import asyncio
import os
import sys

# Add project root to path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.resumes.services.summary import summary_service
from app.resumes.schemas import SummaryType

# 1. 테스트를 위한 가짜 데이터(Mock Data) 설정
# 엔티티에 없는 tech_stack 필드를 제거
mock_data = {
    "resume_id": 1,
    "basic_info": {
        "title": "테스트 개발자",
        "tagline": "성장을 갈망하는 백엔드 엔지니어",
        "re_stack": ["Python", "Java", "Spring Boot"], # 전체 보유 스택은 유지
        "field": "RESUME",
        "preference": {
            "location": "서울 전체",
            "salary": "3,400만 원 이상",
            "employment_type": "FULL_TIME"
        }
    },
    "content": "6개월간의 부트캠프를 통해 성장한 신입 개발자입니다.",
    "projects": [
        {
            "project_name": "영양제 추천 서비스",
            "period": "2025.11 - 2026.01",
            # tech_stack 필드 삭제 (엔티티에 없으므로)
            "contribution": "Java와 Spring Boot를 활용한 백엔드 API 및 결제 시스템 구현",
            "description": "MySQL을 사용하여 데이터베이스를 설계했으며, JPA N+1 문제를 해결하여 조회 성능을 30% 개선했습니다."
        }
    ],
    "careers": []
}

async def run_test():
    print("Starting Summary Service Test...\n")

    try:
        # 1. Test TEXT Mode
        print("--- [TEST 1] Narrative Summary (TEXT Mode) ---")
        text_summary = await summary_service.generate_summary(
            data=mock_data,
            type="RESUME", 
            summary_type=SummaryType.TEXT
        )
        print("\n[Result]:")
        try:
            print(text_summary)
        except Exception:
            print(text_summary.encode('utf-8', errors='ignore').decode('utf-8'))
            
        print("\n[OK] Text Summary Test Completed\n")

        # 2. Test STRUCTURED Mode
        print("--- [TEST 2] Structured Summary (STRUCTURED Mode) ---")
        structured_summary = await summary_service.generate_summary(
            data=mock_data,
            type="RESUME", 
            summary_type=SummaryType.STRUCTURED
        )
        print("\n[Result]:")
        try:
            print(structured_summary)
        except Exception:
             print(structured_summary.encode('utf-8', errors='ignore').decode('utf-8'))

        print("\n[OK] Structured Summary Test Completed")

    except Exception as e:
        print(f"\n[Error] Test Failed: {e}")
        print("Tip: Check if OPENAI_API_KEY is set in your environment or .env file.")

if __name__ == "__main__":
    asyncio.run(run_test())
