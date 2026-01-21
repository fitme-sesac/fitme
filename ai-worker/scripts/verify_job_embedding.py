import sys
import os
import io

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# 프로젝트 루트 경로 추가 (app 모듈 import 위함)
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from fastapi.testclient import TestClient
from app.main import app
from app.core.database import get_db_connection
import logging

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("VerifyScript")

def setup_dummy_job(title_suffix=""):
    """테스트용 Employer 및 Job Posting 생성 Helper"""
    conn = get_db_connection()
    conn.autocommit = True
    job_id = None
    try:
        with conn.cursor() as cur:
            # 1. Employer (Industry 필수)
            cur.execute("SELECT employer_id FROM employer WHERE industry IS NOT NULL LIMIT 1")
            emp_row = cur.fetchone()
            if emp_row:
                employer_id = emp_row[0]
            else:
                cur.execute("""
                    INSERT INTO employer (name, industry, employee_count, status, employer_uid)
                    VALUES ('Test Corp', 'AI/Robot', 10, 'ACTIVE', gen_random_uuid())
                    RETURNING employer_id
                """)
                employer_id = cur.fetchone()[0]

            # 2. Job Posting
            cur.execute("""
                INSERT INTO job_posting (
                    employer_id, title, description, stack, status, summary
                ) VALUES (
                    %s, %s, 
                    '테스트용 공고 설명입니다.', 
                    'Python, SQL', 'OPEN', '요약'
                ) RETURNING job_id
            """, (employer_id, f"Test Developer {title_suffix}"))
            job_id = cur.fetchone()[0]
            print(f"   🛠️ [Setup] 테스트용 Job ID 생성 완료: {job_id}")
            return job_id
    finally:
        conn.close()

def check_db_embedding(job_id):
    """DB에 임베딩이 업데이트 되었는지 확인"""
    conn = get_db_connection()
    # [Fix] ensure we read committed data by explicit commit or autocommit
    conn.autocommit = True 
    try:
        with conn.cursor() as cur:
            cur.execute(f"SELECT embedding, updated_at FROM job_posting WHERE job_id = {job_id}")
            row = cur.fetchone()
            
            # Debugging
            if row:
                val = row[0]
                # updated_at = row[1]
                
                if val:
                    print(f"   ✅ [DB 확인] Job ID {job_id}: 임베딩 저장 완료 (Vector Not Null)")
                    return True
                else:
                    print(f"   ❌ [DB 확인] Job ID {job_id}: 임베딩이 NULL입니다.")
                    return False
            else:
                print(f"   ❌ [DB 확인] Job ID {job_id}: Row를 찾을 수 없습니다.")
                return False
    finally:
        conn.close()

def get_existing_job_id():
    """DB에서 가장 최근의 Job ID를 가져옵니다 (기존 데이터 테스트용)"""
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT job_id FROM job_posting WHERE deleted_at IS NULL ORDER BY job_id DESC LIMIT 1")
            row = cur.fetchone()
            if row:
                print(f"   👉 [Existing] DB에서 가장 최근 Job ID {row[0]} 를 선택했습니다.")
                return row[0]
            else:
                print("   ❌ [Existing] DB에 조회 가능한 공고가 없습니다.")
                return None
    finally:
        conn.close()

def verify_job_embedding():
    client = TestClient(app)
    print("\n🚀 [검증 시작] 채용 공고 임베딩 - 두 가지 실행 모드 검증\n")

    # ==============================================================================
    # SCENARIO A: DB 조회 방식 (ID만 전달 -> 서버가 DB Join해서 데이터 확보 -> 업데이트)
    # ==============================================================================
    print("🅰️  [Scenario A] DB 조회 방식 (POST /jobs/{id}/embedding)")
    # [Option 1] 매번 새로운 더미 데이터 생성 (기본)
    job_id_a = setup_dummy_job("(Target: DB Lookup)")

    # [Option 2] 기존 DB에 있는 **특정 공고**를 테스트하고 싶으면 아래에 ID를 적고 주석을 푸세요!
    # job_id_a = 123  # <-- 여기에 테스트하고 싶은 실제 Job ID를 입력하세요.
    # if job_id_a is None:
    #      job_id_a = get_existing_job_id() # ID 입력을 안 했으면 그냥 가장 최신거 가져옴

    if job_id_a:
        try:
            print(f"   👉 요청: ID={job_id_a} 전송 (서버가 DB에서 Title, Industry 등을 조회함)")
            res = client.post(f"/jobs/{job_id_a}/embedding")
            
            if res.status_code == 200:
                print("   ✅ API 호출 성공")
                check_db_embedding(job_id_a)
            else:
                print(f"   ❌ API 실패: {res.status_code} {res.text}")
        except Exception as e:
            print(f"   🛑 에러: {e}")

    print("-" * 60)

    # ==============================================================================
    # SCENARIO B: Payload 방식 (Body 전체 전달 -> 서버가 받은 데이터로 생성 -> DB 업데이트)
    # ==============================================================================
    print("🅱️  [Scenario B] Payload 방식 (POST /jobs/embedding)")
    
    # [Option 1] 매번 새로운 더미 데이터 생성 (기본)
    # job_id_b = setup_dummy_job("(Target: Payload Update)")

    # [Option 2] 기존 DB에 있는 ID 사용하고 싶으면 위를 주석하고 아래 주석을 푸세요!
    # job_id_b = get_existing_job_id() 
    job_id_b = 100  # 또는 특정 ID 직접 입력
    
    if job_id_b:
        try:
            # DB에 있는 데이터(Test Developer...)와 상관없이, Body에 실어 보낸 내용으로 임베딩 생성
            payload = {
                "job_id": job_id_b,
                "title": "Backend Developer",
                "stack": ["TailwindCSS", "HuggingFace", "MySQL"],
                "industry": "핀테크 (FinTech)",
                "description": """[주요 업무]
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
• 마감일: 2026-09-11"""
            }
            print(f"   👉 요청: Body 전체 전송 (Job ID: {job_id_b})")
            print(f"       (Title: {payload['title']}, Industry: {payload['industry']})")
            
            res = client.post("/jobs/embedding", json=payload)
            
            if res.status_code == 200:
                print("   ✅ API 호출 성공 (Vector Generated)")
                check_db_embedding(job_id_b)
            else:
                print(f"   ❌ API 실패: {res.status_code} {res.text}")
        except Exception as e:
            print(f"   🛑 에러: {e}")

    print("\n🏁 [검증 종료]")

if __name__ == "__main__":
    verify_job_embedding()
