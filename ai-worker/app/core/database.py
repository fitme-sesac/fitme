import psycopg2
from pgvector.psycopg2 import register_vector
from app.core.config import settings

def get_db_connection():
    """
    Create a PostgreSQL database connection using the application's configured DB settings.
    
    Returns:
        psycopg2.connection: A connection object connected to the configured database.
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
    Register the pgvector extension on the given PostgreSQL connection.
    
    Parameters:
        conn: A psycopg2 database connection on which to register the pgvector extension.
    """
    register_vector(conn)