import os
import psycopg2
from dotenv import load_dotenv

load_dotenv()

host = os.getenv("DB_HOST")
port = os.getenv("DB_PORT")
dbname = os.getenv("DB_NAME")
user = os.getenv("DB_USER")
password = os.getenv("DB_PASSWORD")

print(f"Attempting to connect to {dbname} at {host}:{port} as {user}")

try:
    conn = psycopg2.connect(
        host=host,
        port=port,
        dbname=dbname,
        user=user,
        password=password
    )
    print("SUCCESS: Database connection established.")
    conn.close()
except Exception as e:
    print(f"FAILURE: Could not connect to database. Error: {e}")
