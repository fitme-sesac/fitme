from fastapi.testclient import TestClient
from app.main import app
import os
import json

client = TestClient(app)

# [CI/CD용] 통합 테스트 파일
# GitHub Actions에서 'pytest' 명령어로 실행되는 파일입니다.
# 서버를 띄우지 않고도 FastAPI 앱을 직접 테스트할 수 있습니다.

def test_resume_process_flow():
    """
    [Scenario] 이력서 요약 및 임베딩 생성 전체 흐름 테스트
    """
    API_URL = "/resumes/process"
    
    # 1. 가짜 요청 데이터 구성 (Golden Fix와 동일)
    payload = {
        "resume_id": 999, # 테스트용 ID
        "basic_info": {
            "title": "[CI Test] 백엔드 개발자",
            "re_stack": ["Python", "ValidStack"],
            "field": "RESUME",
            "preference": {
                "location": "서울",
                "salary": "3,000만 원 이상",
                "employment_type": "FULL_TIME"
            }
        },
        "content": "CI/CD 파이프라인 테스트를 위한 자기소개서 내용입니다.",
        "file_links": [], # PDF 없이 텍스트 모드 테스트
        "projects": [
            {
                "project_name": "CI Test Project",
                "start_date": "2024.01",
                "end_date": "2024.02",
                "total_tech_stack": ["Python", "Pytest"],
                "contribution": "테스트 자동화 구현",
                "description": "GitHub Actions 연동 테스트"
            }
        ],
        "careers": [],
        "summary_type": "STRUCTURED",
        "include_reasoning": True
    }

    # 2. API 호출 (Client 사용)
    response = client.post(API_URL, json=payload)
    
    # 3. 검증
    # - 상태 코드 200 OK
    assert response.status_code == 200, f"Failed: {response.text}"
    
    result = response.json()
    
    # - 응답 구조 확인
    assert "summary" in result
    assert "vector_status" in result
    assert result["vector_status"] == "generated"
    
    print("\n✅ CI Flow Test Passed!")
