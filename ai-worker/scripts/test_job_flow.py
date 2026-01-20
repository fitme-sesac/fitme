import json
import os
import sys
from fastapi.testclient import TestClient

# 프로젝트 루트 경로 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

# [Improvement] 서버를 따로 띄우지 않아도 되도록 TestClient 사용
from app.main import app

client = TestClient(app)

def test_job_embedding():
    API_URL = "/jobs/embedding"
    
    print("\n[Step 1] Job Posting Embedding 테스트 시작")
    print(f"Target URL: {API_URL} (Using TestClient)")
    
    # Mock Data (Text Description)
    payload_text = {
        "job_id": 101,
        "title": "백엔드 서비스 개발자 (Backend Developer)",
        "stack": ["Java", "Spring Boot", "MySQL", "AWS"],
        "industry": "핀테크 (FinTech)",
        "description": """
        [주요 업무]
        - 대규모 트래픽 처리를 위한 서버 설계 및 개발
        - 결제 및 정산 시스템 고도화
        - 레거시 시스템 리팩토링 및 MSA 전환
        
        [자격 요건]
        - Java/Spring Boot 기반 백엔드 개발 경력 3년 이상
        - RDBMS (MySQL) 및 NoSQL (Redis) 사용 경험
        - 대용량 트래픽 처리 및 성능 최적화 경험
        """,
        "company_name": "토스뱅크",
        "location": "서울 강남구",
        "salary_text": "5,000만원 이상 (협의 가능)"
    }
    
    # Mock Data (PDF Link Simulation)
    base_dir = os.path.dirname(os.path.dirname(__file__)) # ai-worker/
    pdf_path = os.path.join(base_dir, "temp_pdfs", "이력서_박지영.pdf")
    
    payload_pdf = {
        "job_id": 102,
        "title": "AI 모델링 엔지니어",
        "stack": ["Python", "PyTorch", "TensorFlow"],
        "industry": "인공지능 솔루션",
        "description": pdf_path,
        "company_name": "업스테이지",
        "location": "경기도 판교",
        "salary_text": "6,000만원 이상"
    }

    print("\n[Case 1] 텍스트 기반 공고 테스트...")
    _send_request(API_URL, payload_text)

    print("\n[Case 2] PDF(Link) 기반 공고 테스트...")
    print(f"👉 PDF Path: {pdf_path}")
    if not os.path.exists(pdf_path):
        print("⚠️ 주의: 해당 경로에 PDF 파일이 없습니다. (파일 로드 실패 케이스로 테스트됩니다)")
    
    _send_request(API_URL, payload_pdf)

def _send_request(url, payload):
    try:
        # requests.post -> client.post 변경
        response = client.post(url, json=payload)
        
        if response.status_code == 200:
            print("✅ [Success] API 요청 성공!")
            result = response.json()
            
            print(f"   - Job ID: {result['job_id']}")
            print(f"   - Vector Status: {result['vector_status']}")
            print(f"   - Embedding Dim: {result['embedding_dim']}")
            # vector 출력은 너무 기니까 앞부분만
            vector_preview = result.get('vector', [])[:5]
            print(f"   - Vector Sample: {vector_preview}...")
            
        else:
            print(f"❌ [Failure] Status: {response.status_code}")
            print(f"   Response: {response.text}")
            
    except Exception as e:
        print(f"❌ [Error] 테스트 실패: {e}")

if __name__ == "__main__":
    test_job_embedding()
