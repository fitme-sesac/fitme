
"""
[디버깅용 스크립트]
psycopg2가 윈도우 환경에서 한글 에러 메시지를 디코딩하지 못하고 죽는 문제를 우회하기 위해,
순수 파이썬 드라이버인 `pg8000`을 사용하여 진짜 에러 내용(예: DB 없음, 비번 틀림)을 확인하려는 용도입니다.
"""
import pg8000.native
import os
from dotenv import load_dotenv
import sys
import io

# 터미널 인코딩 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

print("Loading .env...")
load_dotenv(encoding="utf-8")

host = os.getenv("DB_HOST", "127.0.0.1")
if host == 'localhost': host = '127.0.0.1'
port = int(os.getenv("DB_PORT", 5432))
dbname = os.getenv("DB_NAME", "fitme_project")  # Update default to fitme_project
user = os.getenv("DB_USER", "postgres")
password = os.getenv("DB_PASSWORD", "password")

print(f"Connecting to '{dbname}' at {host}:{port} as '{user}' using pg8000...")
print(f"Password length: {len(password)}")

try:
    conn = pg8000.native.Connection(
        user=user,
        password=password,
        host=host,
        port=port,
        database=dbname
    )
    print("SUCCESS! Connected with pg8000.")
    conn.close()
except Exception as e:
    print("FAILURE in pg8000!")
    print(f"Error: {e}")
