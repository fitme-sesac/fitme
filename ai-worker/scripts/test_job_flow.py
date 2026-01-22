import json
import os
import sys
import io

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

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
        "job_id": 100,
        "title": "Backend Developer",
        "stack": ["TailwindCSS", "HuggingFace", "MySQL"],
        "industry": "핀테크 (FinTech)",
        "description": """
        [주요 업무]
        • 이데아게임즈 주식회사의 핵심 백엔드 시스템 설계 및 개발
        • RESTful API 및 마이크로서비스 아키텍처 구축
        • 대용량 트래픽 처리를 위한 시스템 최적화
        • 데이터베이스 설계 및 쿼리 최적화
        • 코드 리뷰 및 기술 문서 작성
        
        [자격 요건]
        • 4년 이상 경력
        • TailwindCSS, HuggingFace, MySQL 기술 스택 활용 경험
        • 객체지향 프로그래밍 및 디자인 패턴 이해
        • Git 기반 협업 경험
        
        [우대 사항]
        • MSA(Microservice Architecture) 설계 및 운영 경험
        • 대용량 트래픽 처리 경험
        • CI/CD 파이프라인 구축 경험
        • 오픈소스 기여 경험
        
        [근무 조건]
        • 연봉: 8399만원
        • 근무지: 부산광역시
        • 마감일: 2026-09-11
        """
    }
    
    # Mock Data (PDF Link Simulation)
    base_dir = os.path.dirname(os.path.dirname(__file__)) # ai-worker/
    pdf_path = os.path.join(base_dir, "temp_pdfs", "이력서_박지영.pdf")
    
    payload_pdf = {
        "job_id": 102,
        "title": "AI 모델링 엔지니어",
        "stack": ["Python", "PyTorch", "TensorFlow"],
        "industry": "인공지능 솔루션",
        "description": pdf_path
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
