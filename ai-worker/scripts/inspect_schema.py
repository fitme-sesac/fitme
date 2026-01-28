import psycopg2
import os
from dotenv import load_dotenv
import sys
import io

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

load_dotenv()

host = os.getenv("DB_HOST", "127.0.0.1")
if host == 'localhost': host = '127.0.0.1' 
port = os.getenv("DB_PORT", "5432")
user = os.getenv("DB_USER", "postgres")
password = os.getenv("DB_PASSWORD", "password")
dbname = os.getenv("DB_NAME", "fitme_project")

print(f"Connecting to {dbname}...")

try:
    conn = psycopg2.connect(
        host=host, port=port, user=user, password=password, dbname=dbname
    )
    cur = conn.cursor()
    
    # 1. 테이블 목록 조회
    print("\n[Tables]")
    cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
    tables = cur.fetchall()
    for t in tables:
        print(f"- {t[0]}")
        
    # 2. Resume 테이블 컬럼 조회
    print("\n[Columns in 'resume' table]")
    cur.execute("""
        SELECT column_name, data_type, udt_name
        FROM information_schema.columns 
        WHERE table_name = 'resume'
    """)
    columns = cur.fetchall()
    for c in columns:
        print(f"- {c[0]} ({c[1]}, {c[2]})")

    cur.close()
    conn.close()

except Exception as e:
    print(f"Error: {e}")
