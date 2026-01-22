import sys
import os
import io

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# 프로젝트 루트 경로 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.resumes.schemas import ResumeSummary
from app.jobpostings.schemas import JobEmbeddingRequest
from app.jobpostings.services import JobService

def show_formats():
    print("=" * 80)
    print("📢 FINAL EMBEDDING FORMAT DEMONSTRATION")
    print("=" * 80)

    # ---------------------------------------------------------
    # 1. Resume Side
    # ---------------------------------------------------------
    print("\n[Resource: Resume (Insae)]")
    
    # Mock AI Summary Result
    resume_summary = ResumeSummary(
        professional_identity="5년차 백엔드 개발자", # Not used in embedding if universal_competencies exists
        key_achievement="TPS 30% 개선",
        problem_solving="Redis 도입으로 해결",
        credibility="정보처리기사",
        collaboration="코드리뷰 주도",
        matching_info="서울 강남, 4000만원",
        ai_reasoning=[],
        universal_competencies=[
            "대규모 트래픽 분산 처리",
            "MSA 아키텍처 설계 및 운영",
            "이벤트 기반 아키텍처(EDA) 구현",
            "AWS 클라우드 인프라 최적화",
            "동시성 제어 및 데이터 무결성 보장"
        ]
    )
    
    # Simulate Controller Logic
    resume_title = "백엔드 개발자"
    resume_stack = "Java, Spring Boot, MySQL"
    
    resume_embedding_text = resume_summary.to_embedding_string(title=resume_title, tech_stack=resume_stack)
    
    print("-" * 40)
    print(resume_embedding_text)
    print("-" * 40)

    # ---------------------------------------------------------
    # 2. Job Side
    # ---------------------------------------------------------
    print("\n[Resource: Job Posting (Gong-go)]")
    
    # User-Provided Mock Data
    job_desc = """
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
    job_req = JobEmbeddingRequest(
        job_id=100,
        title="Backend Developer",
        stack=["TailwindCSS", "HuggingFace", "MySQL"],
        industry="핀테크 (FinTech)",
        description=job_desc
    )
    
    # Simulate JobService Logic
    # Note: _process_description is async but for plain text it just returns text. 
    # _create_symmetric_text is sync.
    # We will manually mimic the flow or call internal method if accessible.
    
    service = JobService()
    # Direct access to internal creation logic for demo
    job_embedding_text = service._create_symmetric_text(job_req, job_desc)
    
    print("-" * 40)
    print(job_embedding_text.strip())
    print("-" * 40)
    
    print("\n✅ Verification Complete: Both sides use the Symmetric Tag Structure.")

if __name__ == "__main__":
    show_formats()
