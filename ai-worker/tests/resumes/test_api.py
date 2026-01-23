"""
이 파일은 FastAPI 서버의 `/resumes/process` 엔드포인트를 테스트하기 위한 스크립트입니다.
실제 프론트엔드에서 보낼 법한 JSON 데이터(한글 포함)를 가상으로 만들어 서버에 전송합니다.
- json형식으로 받는 버전의 테스트

실행 방법:
> python tests/resumes/test_api.py
"""
import requests
import json

# URL 변경: /test/process-resume -> /resumes/process
url = "http://localhost:8000/resumes/process"

# 테스트용 데이터 정의
data = {
    "id": 1,
    "type": "RESUME",
    "data": {
        "experience": [
            {
                "company": "핀테크 코퍼레이션",
                "role": "백엔드 개발자",
                "duration": "42개월",
                "is_verified": True,
                "description": "결제 시스템 고도화 및 운영",
                "projects": [
                    {
                        "name": "실시간 입찰 엔진",
                        "contribution_pct": 85,
                        "description": "실시간 크레딧 결제 로직 설계 및 구현"
                    }
                ]
            }
        ],
        "skills": ["Java 17", "Spring Boot", "PostgreSQL", "pgvector"],
        "ai_description": "분산 락(Distributed Lock)을 도입하여 결제 데이터 정합성을 확보하고, Redis 캐싱 전략을 통해 DB 부하를 40% 절감했습니다.",
        "certifications": ["AWS Solutions Architect Associate"],
        "links": ["https://github.com/example"],
        "culture": "코드 리뷰를 통한 동반 성장, 기술적 도전"
    }
}

try:
    print(f"Sending request to {url}...")
    response = requests.post(url, json=data)
    print(f"Status Code: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print("Success!")
        print("Summary:")
        print(result["summary"])
    else:
        print("Error response:")
        print(response.text)
except Exception as e:
    print(f"Connection failed: {e}")
    # 상위 디렉토리(루트)에서 실행해야 하므로 경로는 이렇게 안내
    print("Make sure the FastAPI server is running: uvicorn app.main:app --reload")
