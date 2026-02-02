import asyncio
import random
import uuid
from datetime import datetime
import asyncpg
import redis.asyncio as redis
import json
import sys
import io
from dotenv import load_dotenv
import os
from urllib.parse import quote_plus

# 터미널 한글 출력 설정 (윈도우 환경 대응)
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# .env.docker 파일 로드 (Docker 환경 설정 우선)
# 프로젝트 루트 디렉토리를 찾아 .env.docker 파일을 가져옵니다.
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.abspath(os.path.join(current_dir, "../.."))
env_docker_path = os.path.join(project_root, ".env.docker")

if os.path.exists(env_docker_path):
    load_dotenv(env_docker_path)
    print(f"[설정] .env.docker 파일을 성공적으로 불러왔습니다.")
else:
    load_dotenv()
    print(f"[주의] .env.docker 파일이 없어서 기본 .env를 시도합니다.")

# =============================================================================
# [설정 정보 및 호스트 보정]
# .env.docker 파일은 도커 내부 네트워킹을 위해 호스트가 'redis', 'postgres'로 되어있음
# 로컬 환경에서 실행 시 이를 'localhost'로 보정합니다.
# =============================================================================
def get_env_host(key, default):
    val = os.getenv(key, default)
    if val in ["redis", "postgres"]:
        return "127.0.0.1"
    return val

DB_CONFIG = {
    "user": os.getenv("DB_USER", "postgres"),
    "password": os.getenv("DB_PASSWORD"),
    "database": os.getenv("DB_NAME", "fitme_project"),
    "host": get_env_host("DB_HOST", "127.0.0.1"),
    "port": os.getenv("DB_PORT", "5432")
}

# Redis 설정 정보 로드
REDIS_HOST = get_env_host("REDIS_HOST", "127.0.0.1")
REDIS_PORT = os.getenv("REDIS_PORT", "6379")
REDIS_PASS = os.getenv("REDIS_PASSWORD")

# 비밀번호에 특수문자가 있을 경우를 대비해 URL 인코딩 (/, + 등 처리)
encoded_pass = quote_plus(REDIS_PASS)
REDIS_URL = f"redis://default:{encoded_pass}@{REDIS_HOST}:{REDIS_PORT}"

TARGET_EMPLOYER_COUNT = 50     # 생성할 기업(광고주) 수
TARGET_JOB_COUNT = 300000      # 생성할 채용공고 및 광고 수 (30만 건)
BATCH_SIZE = 1000              # 한 번에 DB에 밀어넣을 단위 (성능 최적화)
VECTOR_DIM = 1536              # OpenAI text-embedding-3-small 모델의 벡터 차원 수

print(f"[Step 1] 데이터 생성 엔진 초기화 (목표: {TARGET_JOB_COUNT}개 광고)...")

async def main():
    # PostgreSQL 및 Redis 비동기 연결
    conn = await asyncpg.connect(**DB_CONFIG)
    r = redis.from_url(REDIS_URL)

    try:
        # =====================================================================
        # [Step 1.5] DB 시퀀스 리셋 (ID 중복 에러 방지)
        # =====================================================================
        # 데이터를 직접 삽입할 경우, DB의 자동 번호 생성기(Sequence)가 
        # 마지막 ID값을 인식하지 못할 수 있어 수동으로 동기화해줍니다.
        print(f"[Step 1.5] DB 시퀀스(번호표) 동기화 중...")
        tables_pk = [
            ("member", "member_id"),
            ("employer", "employer_id"),
            ("wallet", "wallet_id"),
            ("job_posting", "job_id"),
            ("ad_campaign", "campaign_id"),
            ("resume", "resume_id"),
            ("employer_member", "employer_member_id")
        ]
        
        for table, pk in tables_pk:
            try:
                # setval: 시퀀스 값을 해당 테이블의 MAX(ID) + 1로 설정
                await conn.execute(f"""
                    SELECT setval(pg_get_serial_sequence('{table}', '{pk}'), COALESCE(MAX({pk}), 0) + 1, false) FROM {table};
                """)
            except Exception as e:
                print(f"[Warning] {table} 시퀀스 리셋 실패 (무시 가능): {e}")
        print(f"[Check] 시퀀스 동기화 완료.")

        # =====================================================================
        # [Step 2] 기업(광고주) 데이터 생성
        # =====================================================================
        # 기업 계정 생성 -> 기업 정보 생성 -> 계정-기업 연결 -> 광고 예산용 지갑 생성을 순차 진행
        print(f"[Step 2] {TARGET_EMPLOYER_COUNT}개의 기업 데이터 생성 중...")
        employer_ids = []
        
        for i in range(TARGET_EMPLOYER_COUNT):
            # 1. 회원(Member) 생성
            member_uid = uuid.uuid4()
            email = f"load_test_{i}_{uuid.uuid4().hex[:4]}@example.com"
            login_id = f"load_user_{i}_{uuid.uuid4().hex[:6]}"
            
            # 개인정보 동의 및 약관 알림 ID(1번)를 포함하여 INSERT (Check 제약조건 준수)
            member_id = await conn.fetchval("""
                INSERT INTO member 
                (member_uid, email, login_id, password_hash, name, role, status, 
                 auth_provider, marketing_opt_in, failed_login_count, created_at, updated_at,
                 terms_agreed_at, privacy_agreed_at, policy_agreed_at,
                 terms_notice_id, privacy_notice_id, policy_notice_id)
                VALUES ($1, $2, $3, 'pass_hash', $4, 'EMPLOYER', 'ACTIVE', 
                        'OTHER', false, 0, NOW(), NOW(),
                        NOW(), NOW(), NOW(),
                        1, 1, 1)
                RETURNING member_id
            """, member_uid, email, login_id, f"EmpUser {i}")

            # 2. 기업(Employer) 생성
            employer_uid = uuid.uuid4()
            emp_name = f"LoadTest Corp {i}"
            
            emp_id = await conn.fetchval("""
                INSERT INTO employer 
                (employer_uid, name, status, created_at, updated_at)
                VALUES ($1, $2, 'ACTIVE', NOW(), NOW())
                RETURNING employer_id
            """, employer_uid, emp_name)
            
            employer_ids.append(emp_id)

            # 3. 연결 유저 지정 (EmployerMember)
            await conn.execute("""
                INSERT INTO employer_member
                (employer_id, member_id, role_in_company, active, created_at)
                VALUES ($1, $2, 'OWNER', true, NOW())
            """, emp_id, member_id)

            # 4. 광고주 전용 지갑(Wallet) 생성
            # 테스트를 위해 10억 원의 넉넉한 예산을 할당합니다.
            await conn.execute("""
                 INSERT INTO wallet 
                 (employer_id, owner_type, balance, status, created_at, updated_at)
                 VALUES ($1, 'EMPLOYER', 1000000000, 'ACTIVE', NOW(), NOW())
            """, emp_id)

        print(f"[Check] {len(employer_ids)}개 기업 및 지갑 생성 완료.")

        # =====================================================================
        # [Step 3 & 4] 채용공고 및 광고 대량 생성 (Bulk Insert)
        # =====================================================================
        # 5만 건의 데이터를 하나씩 넣으면 매우 느리므로, unnest 기법을 사용해 1000개씩 묶어서 처리합니다.
        print(f"[Step 3] {TARGET_JOB_COUNT}개의 채용공공 및 광고 캠페인 생성 시작 (배치 크기: {BATCH_SIZE})...")
        
        locations = ["서울 강남구", "서울 서초구", "경기 성남시", "부산 해운대구", "대구 수성구", "대전 유성구", "광주 서구"]
        stacks = [["Java", "Spring"], ["Python", "Django"], ["Node.js", "React"], ["Go", "Kubernetes"], ["AWS", "DevOps"]]

        total_batches = (TARGET_JOB_COUNT + BATCH_SIZE - 1) // BATCH_SIZE
        
        for batch_idx in range(total_batches):
            # 1개 배치(Batch)용 데이터 준비
            current_batch_size = min(BATCH_SIZE, TARGET_JOB_COUNT - (batch_idx * BATCH_SIZE))
            
            job_data_employer_ids = []
            job_data_titles = []
            job_data_locations = []
            job_data_stacks = []
            job_data_vectors = []
            
            for _ in range(current_batch_size):
                emp_id = random.choice(employer_ids)
                job_data_employer_ids.append(emp_id)
                job_data_titles.append(f"부하 테스트 공고 {batch_idx}_{uuid.uuid4().hex[:6]}")
                job_data_locations.append(random.choice(locations))
                job_data_stacks.append(random.choice(stacks))
                # 1536차원의 랜덤 벡터 생성 (유사도 검색 테스트용)
                job_data_vectors.append(str([random.random() for _ in range(VECTOR_DIM)]))

            # PostgreSQL 배열 타입 처리를 위해 기술 스택 리스트를 콤마(,) 문자열로 변환
            job_data_stacks_str = [ ",".join(s) for s in job_data_stacks ]

            # 채용공고 대량 삽입 (unnest 기법 활용으로 속도 극대화)
            job_rows = await conn.fetch("""
                INSERT INTO job_posting 
                (employer_id, title, description, summary, location, salary_text, status, stack, embedding, created_at, updated_at)
                SELECT x.eid, x.title, '부하 테스트용 공고 상세 설명입니다.', '공고 요약 블라블라...', x.loc, '연봉협의', 'OPEN', string_to_array(x.stack, ','), x.vec::vector, NOW(), NOW()
                FROM unnest($1::bigint[], $2::text[], $3::text[], $4::text[], $5::text[]) 
                AS x(eid, title, loc, stack, vec)
                RETURNING job_id, employer_id
            """, job_data_employer_ids, job_data_titles, job_data_locations, job_data_stacks_str, job_data_vectors)

            # 광고 캠페인 데이터 준비
            ad_job_ids = []
            ad_emp_ids = []
            ad_cpcs = []
            ad_budgets = []
            
            # Redis 동기화를 위한 파이프라인(Pipeline) 생성
            # 네트워크 통신 횟수를 줄여 Redis 삽입 속도를 높여줍니다.
            pipe = r.pipeline() 

            for row in job_rows:
                ad_job_ids.append(row['job_id'])
                ad_emp_ids.append(row['employer_id'])
                ad_cpcs.append(random.randint(100, 2000)) # 입찰가 랜덤 설정 (100~2000원)
                ad_budgets.append(1000000)                # 일일 예산 100만 원

            # 광고 캠페인 대량 삽입
            camp_rows = await conn.fetch("""
                INSERT INTO ad_campaign
                (employer_id, job_id, cpc_bid, daily_budget, status, start_at, end_at, created_at, updated_at)
                SELECT x.eid, x.jid, x.cpc, x.budget, 'ACTIVE', NOW(), NOW() + interval '30 days', NOW(), NOW()
                FROM unnest($1::bigint[], $2::bigint[], $3::int[], $4::bigint[]) 
                AS x(eid, jid, cpc, budget)
                RETURNING campaign_id, cpc_bid, daily_budget
            """, ad_emp_ids, ad_job_ids, ad_cpcs, ad_budgets)

            # Redis Guard 데이터 동기화 (V2 방식 테스트용)
            for row in camp_rows:
                cid = row['campaign_id']
                cpc = row['cpc_bid']
                budget = row['daily_budget']
                
                # 광고 상태 및 예산/입찰가 정보를 Hash로 저장
                guard_key = f"ad_state:{cid}"
                pipe.hset(guard_key, mapping={
                    "budget": str(budget),
                    "cpc": str(cpc),
                    "status": "ACTIVE"
                })
                # 활성 광고 목록(ZSet)에 ID 추가 (Score=CPC)
                # Redis Sorted Set을 사용하여 상위 입찰가 광고를 빠르게 추출할 수 있게 합니다.
                pipe.zadd("ad:active_campaigns_zset", {str(cid): cpc})

            # Redis 명령어 묶음 일괄 실행
            await pipe.execute()
            
            print(f"   -> 배치 {batch_idx+1}/{total_batches} 완료 ({current_batch_size}개 데이터 생성됨)")

        print(f"[Check] 총 {TARGET_JOB_COUNT}개의 광고 캠페인이 DB 및 Redis에 생성되었습니다.")
        
        # =====================================================================
        # [Step 5] 테스트용 유저 및 이력서 생성 (유사도 검색용)
        # =====================================================================
        test_email = "test_user_v2@example.com"
        
        exists = await conn.fetchval("SELECT member_id FROM member WHERE email = $1", test_email)
        test_uid = exists
        if not test_uid:
            # 테스트 유저 생성
            test_uid = await conn.fetchval("""
                INSERT INTO member 
                (member_uid, email, login_id, password_hash, name, role, status, 
                 auth_provider, marketing_opt_in, failed_login_count, created_at, updated_at,
                 terms_agreed_at, privacy_agreed_at, policy_agreed_at,
                 terms_notice_id, privacy_notice_id, policy_notice_id)
                VALUES ($1, $2, 'test_user_v2', 'pass_hash', '부하테스트용 구직자', 'CANDIDATE', 'ACTIVE', 
                        'OTHER', false, 0, NOW(), NOW(),
                        NOW(), NOW(), NOW(),
                        1, 1, 1)
                RETURNING member_id
            """, uuid.uuid4(), test_email)
            
            # 테스트용 이력서 및 벡터 생성
            resume_vector = str([random.random() for _ in range(VECTOR_DIM)])
            await conn.execute("""
                INSERT INTO resume (member_id, title, field, content, is_primary, embedding, created_at, updated_at)
                VALUES ($1, '백엔드 개발자 테스트 이력서', 'BACKEND', '열정적인 자바 개발자입니다...', true, $2::vector, NOW(), NOW())
            """, test_uid, resume_vector)
            print(f"[Check] 테스트 유저(ID: {test_uid}) 및 이력서 생성 완료.")
        else:
             print(f"[Info] 테스트 유저(ID: {test_uid})가 이미 존재합니다.")

        print(f"\n[부하 테스트 설정 완료]")
        print(f"1. V2 (Redis) 전체 조회 API: GET /api/v2/ad/serve (활성 광고 개수: {await r.scard('ad:active_campaigns')})")
        print(f"2. V2 (Redis) 개인화 매칭 API: GET /api/v2/ad/serve/match?memberId={test_uid}")
        
    finally:
        # DB 및 Redis 연결 종료
        await conn.close()
        await r.aclose()

if __name__ == "__main__":
    # asyncio를 통해 메인 루프 실행
    asyncio.run(main())
