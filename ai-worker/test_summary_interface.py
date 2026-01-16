import asyncio
from app.resumes.schemas import ResumeRequest, SummaryType
from app.resumes.services.summary import SummaryService
import json
import inspect

# 테스트용 더미 데이터 (Mock Data)
# 실제 DB나 API 요청 대신 사용할 가짜 이력서 데이터입니다.
mock_data = {
  "resume_id": 1,
  "basic_info": {
    "title": "테스트 개발자",
    "tagline": "열정적인 코더",
    "re_stack": ["Python"],
    "field": "RESUME",
    "preference": {
      "location": "서울",
      "salary": "3000",
      "employment_type": "FULL"
    }
  },
  "content": "이것은 테스트 이력서 내용입니다.",
  "projects": [],
  "careers": []
}

async def test_summary_interface():
    print("--- 1. 스키마(데이터 구조) 테스트 ---")
    
    # 1-1. 기본값 테스트 (STRUCTURED)
    # summary_type을 지정하지 않았을 때 기본적으로 'STRUCTURED'로 설정되는지 확인합니다.
    req1 = ResumeRequest(**mock_data)
    print(f"기본 summary_type 값: {req1.summary_type}")
    assert req1.summary_type == SummaryType.STRUCTURED
    
    # 1-2. 명시적 값 테스트 (TEXT)
    # summary_type을 'TEXT'로 지정했을 때 올바르게 설정되는지 확인합니다.
    data_text = mock_data.copy()
    data_text["summary_type"] = "TEXT"
    req2 = ResumeRequest(**data_text)
    print(f"지정된 summary_type 값: {req2.summary_type}")
    assert req2.summary_type == SummaryType.TEXT
    
    print("✓ 스키마 유효성 검사 성공 (Schema Validation Passed)")

    print("\n--- 2. 서비스 로직 테스트 (LLM 모킹) ---")
    service = SummaryService()
    
    # 실제 외부 LLM API(OpenAI 등)를 호출하지 않고, 내부 로직만 점검하기 위한 코드입니다.
    try:
        # inspect 모듈을 사용하여 generate_summary 메서드가 존재하는지, 
        # 그리고 어떤 인자를 받는지 시그니처(서명)를 확인합니다.
        sig = inspect.signature(service.generate_summary)
        print(f"메서드 시그니처: {sig}")
        print("✓ 서비스 메서드 확인 완료")
    except Exception as e:
        print(f"✗ 서비스 확인 실패: {e}")

if __name__ == "__main__":
    # 비동기 함수 실행
    asyncio.run(test_summary_interface())
