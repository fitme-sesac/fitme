"""
[디버깅용 스크립트]
DB 연결 시 발생하는 인코딩 문제(UnicodeDecodeError)를 분석하기 위해 생성한 파일입니다.
환경 변수 값을 Hex(16진수)로 출력하여 숨겨진 특수문자가 있는지 확인하고,
psycopg2를 사용하여 직접 연결을 시도합니다.
"""
import os
import psycopg2
from dotenv import load_dotenv
import sys

# 터미널 인코딩 설정 (윈도우 출력 문제 방지)
import io
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# DB 클라이언트 인코딩과 로케일 강제 설정 (에러 메시지 깨짐 방지)
os.environ["PGCLIENTENCODING"] = "utf-8"
os.environ["LC_ALL"] = "C"

print("=== Starting Debug Script ===")

# 1. .env 로드 시도
print("Loading .env with utf-8...")
try:
    load_dotenv(encoding="utf-8")
except Exception as e:
    print(f"Error loading .env: {e}")

# 2. 값 확인 및 HEX 덤프 (숨겨진 문자 확인)
def print_hex(name, value):
    if not value:
        print(f"{name}: None")
        return
    hex_str = " ".join(f"{ord(c):02x}" for c in value)
    print(f"{name}: {value} (Hex: {hex_str})")

host = os.getenv("DB_HOST", "127.0.0.1")
# localhost 대신 127.0.0.1 강제 사용 (IPv6 문제 회피)
if host == 'localhost':
    host = '127.0.0.1'

port = os.getenv("DB_PORT", "5432")
dbname = os.getenv("DB_NAME", "fitme")
user = os.getenv("DB_USER", "postgres")
password = os.getenv("DB_PASSWORD", "password")

print_hex("DB_HOST", host)
print_hex("DB_PORT", port)
print_hex("DB_NAME", dbname)
print_hex("DB_USER", user)
# 비밀번호는 앞글자만 노출하고 나머지는 길이만 표시
pw_display = (password[0] + "*" * (len(password)-1)) if password else "None"
print(f"DB_PASSWORD(Masked): {pw_display}")
print(f"DB_PASSWORD(First 2 Hex): {' '.join(f'{ord(c):02x}' for c in password[:2])}...")

# 3. 직접 연결 시도
print("\nAttempting connection with client_encoding=utf8 and host=127.0.0.1...")
try:
    conn = psycopg2.connect(
        host=host,
        port=port,
        dbname=dbname,
        user=user,
        password=password,
        options="-c client_encoding=utf8"
    )
    print("SUCCESS! Connected to Database.")
    conn.close()
except Exception as e:
    print("FAILURE! Connection failed.")
    print(f"Error Type: {type(e)}")
    print(f"Error Message: {e}")
    import traceback
    traceback.print_exc()

print("=== End of Debug Script ===")
