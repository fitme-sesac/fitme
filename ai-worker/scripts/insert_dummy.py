import psycopg2
import os
from dotenv import load_dotenv
import sys
import io

# 터미널 한글 출력 설정 (윈도우 환경 대응)
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

load_dotenv(encoding="utf-8")

host = os.getenv("DB_HOST", "127.0.0.1")
if host == 'localhost': host = '127.0.0.1' 
port = os.getenv("DB_PORT", "5432")
user = os.getenv("DB_USER", "postgres")
password = os.getenv("DB_PASSWORD", "password")
dbname = os.getenv("DB_NAME", "fitme_project")

print(f"📡 '{dbname}' 데이터베이스에 연결 중...")

try:
    conn = psycopg2.connect(
        host=host, port=port, user=user, password=password, dbname=dbname, options="-c client_encoding=utf8"
    )
    conn.autocommit = True
    cur = conn.cursor()

    # 1. 1번 이력서 존재 여부 확인
    print("🔍 이력서(ID: 1)가 있는지 확인하는 중...")
    cur.execute("SELECT count(*) FROM resume WHERE resume_id = 1")
    count = cur.fetchone()[0]

    if count > 0:
        print("✅ 이미 1번 이력서가 존재합니다.")
        
        # 현재 상태 확인과 요약 내용 일부 출력
        cur.execute("SELECT summary_status, summary FROM resume WHERE resume_id = 1")
        row = cur.fetchone()
        status = row[0]
        summary = row[1]
        
        print(f"   - 현재 상태: {status}")
        if summary:
            print(f"   - 요약 내용(앞부분): {summary[:30]}...")
        else:
            print("   - 요약 내용: 없음 (아직 AI 처리가 안 된 것 같습니다)")
            
    else:
        print("❌ 1번 이력서가 없습니다.")
        print("🛠️ 테스트용 더미 데이터를 생성합니다...")
        
        try:
            # 유효한 member_id 조회
            cur.execute("SELECT member_id FROM member LIMIT 1")
            member_row = cur.fetchone()
            
            if not member_row:
                raise Exception("'member' 테이블에 회원이 한 명도 없습니다. 먼저 회원을 생성해야 합니다.")
            
            target_member_id = member_row[0]
            print(f"👉 기존 회원(ID: {target_member_id})을 찾았습니다. 이 회원의 ID로 이력서를 생성합니다.")

            # 필수 필드만 채워서 삽입 (동적으로 찾은 member_id 사용)
            cur.execute("""
                INSERT INTO resume (
                    resume_id, member_id, title, field, status, created_at, updated_at
                ) VALUES (
                    1, %s, 'AI 테스트용 이력서', 'RESUME', 'ACTIVE', NOW(), NOW()
                )
            """, (target_member_id,))
            print("✅ 더미 데이터(ID: 1)가 성공적으로 추가되었습니다!")
            print("Tip: 이제 'python test_api.py'를 실행하면 DB에 결과가 저장될 겁니다.")
            
        except Exception as insert_e:
            print(f"⚠️ 더미 데이터 추가 실패: {insert_e}")
            print("혹시 'member' 테이블에 ID가 1인 회원이 없나요? 외래키(FK) 제약조건 때문에 실패했을 수 있습니다.")

    cur.close()
    conn.close()

except Exception as e:
    print(f"🛑 에러 발생: {e}")
