import requests
import json
import sys

# Java 백엔드가 보낼 데이터 형식
dummy_payload = {
    "resume_id": 9999,
    "basic_info": {
        "title": "백엔드 개발자 이력서",
        "re_stack": ["Java", "Spring Boot", "AWS", "MySQL"],
        "field": "BACKEND",
        "education": {
            "status": "졸업",
            "major": "컴퓨터공학",
            "school_name": "한국대학교"
        },
        "preference": {
            "location": "서울",
            "salary": "5000",
            "employment_type": "정규직"
        }
    },
    "content": "Spring Boot를 이용한 대규모 트래픽 처리 경험이 있습니다. MSA 아키텍처 설계 및 구현 경험 보유.",
    "projects": [
        {
            "project_name": "E-commerce 플랫폼 리팩토링",
            "start_date": "2023.01",
            "end_date": "2023.12",
            "total_tech_stack": ["Java", "Spring Data JPA", "Redis"],
            "description": "기존 모놀리식 구조를 MSA로 전환하고, Redis 캐싱을 도입하여 조회 성능을 300% 개선했습니다.",
            "contribution": ""
        }
    ],
    "careers": [
        {
            "company_name": "테크 스타트업",
            "role": "서버 개발자",
            "start_date": "2022.01",
            "end_date": "2024.01",
            "description": "API 서버 개발 및 유지보수"
        }
    ],
    "summary_type": "STRUCTURED",
    "include_reasoning": False
}

def test_summary_api():
    url = "http://localhost:8000/resumes/generate-summary"
    print(f"[Test] Sending request to {url}...")
    
    try:
        # requests 라이브러리 사용 (동기)
        response = requests.post(url, json=dummy_payload)
        
        if response.status_code == 200:
            result = response.json()
            print("\n[Success] AI Worker responded!")
            
            summary = result.get('summary', '')
            embedding = result.get('embedding', [])
            eval_info = result.get('eval_info', {})
            
            print(f"- Summary Length: {len(summary)} chars")
            print(f"- Summary Content: {summary[:50]}...")
            print(f"- Embedding Size: {len(embedding)} (Target: 1536)")
            print(f"- Eval Info: {eval_info}")
            
            if len(embedding) == 1536:
                print("\n[PASS] Embedding dimension is correct.")
            else:
                print("\n[FAIL] Embedding dimension mismatch.")
                
        else:
            print(f"\n[Failed] Status Code: {response.status_code}")
            print(f"Detail: {response.text}")
            
    except Exception as e:
        print(f"\n[Error] Connection failed: {e}")
        print("Make sure the Python server is running on port 8000.")

if __name__ == "__main__":
    test_summary_api()
