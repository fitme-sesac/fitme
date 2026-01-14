import psycopg2
from pgvector.psycopg2 import register_vector
from app.core.config import settings

def get_db_connection():
    """
    PostgreSQL 데이터베이스 연결을 생성하여 반환합니다.
    """
    return psycopg2.connect(
        host=settings.DB_HOST,
        port=settings.DB_PORT,
        dbname=settings.DB_NAME,
        user=settings.DB_USER,
        password=settings.DB_PASSWORD
    )

def init_db_extensions(conn):
    """
    pgvector 확장 등을 등록합니다.
    """
    register_vector(conn)
