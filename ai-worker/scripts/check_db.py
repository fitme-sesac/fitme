import os
import psycopg2
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

# 환경 변수에서 DB 연결 정보 로드
host = get_env_host("DB_HOST", "127.0.0.1")
port = os.getenv("DB_PORT", "5432")
dbname = os.getenv("DB_NAME", "fitme_project")
user = os.getenv("DB_USER", "postgres")
password = os.getenv("DB_PASSWORD")

print(f"[DB 접속 테스트] '{dbname}' 데이터베이스({host}:{port})에 '{user}' 계정으로 연결 시도 중...")

try:
    # psycopg2를 사용한 DB 연결 테스트
    conn = psycopg2.connect(
        host=host,
        port=port,
        dbname=dbname,
        user=user,
        password=password
    )
    print("[성공] 데이터베이스 연결이 정상적으로 수립되었습니다.")
    conn.close()
except Exception as e:
    # 연결 실패 시 에러 메시지 출력
    print(f"[실패] 데이터베이스에 연결할 수 없습니다. 에러내용: {e}")
