import psycopg2
import os
from dotenv import load_dotenv
import sys
import io

# 터미널 한글 출력 설정 (윈도우 환경 대응)
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# .env.docker 파일 로드 (Docker 환경 설정 우선)
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.abspath(os.path.join(current_dir, "../.."))
env_docker_path = os.path.join(project_root, ".env.docker")

if os.path.exists(env_docker_path):
    load_dotenv(env_docker_path)
else:
    load_dotenv()

def get_env_host(key, default):
    val = os.getenv(key, default)
    if val in ["redis", "postgres"]:
        return "127.0.0.1"
    return val

# 환경 변수에서 DB 정보 로드
host = get_env_host("DB_HOST", "127.0.0.1")
if host == 'localhost': host = '127.0.0.1' 
port = os.getenv("DB_PORT", "5432")
user = os.getenv("DB_USER", "postgres")
password = os.getenv("DB_PASSWORD")
dbname = os.getenv("DB_NAME", "fitme_project")

print(f"[계정 확인] '{dbname}' 데이터베이스 구조를 확인하는 중...")

try:
    # DB 연결
    conn = psycopg2.connect(
        host=host, port=port, user=user, password=password, dbname=dbname
    )
    cur = conn.cursor()
    
    # 1. 모든 테이블 목록 조회
    print("\n[현재 생성된 테이블 목록]")
    cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
    tables = cur.fetchall()
    for t in tables:
        print(f"- {t[0]}")
        
    # 2. 'resume' 테이블 상세 컬럼 구조 조회 (벡터 타입 확인용)
    print("\n['resume' 테이블 컬럼 상세 정보]")
    cur.execute("""
        SELECT column_name, data_type, udt_name
        FROM information_schema.columns 
        WHERE table_name = 'resume'
    """)
    columns = cur.fetchall()
    for c in columns:
        print(f"- {c[0]} (타입: {c[1]}, 내부명: {c[2]})")

    cur.close()
    conn.close()

except Exception as e:
    print(f"[에러 발생] {e}")
