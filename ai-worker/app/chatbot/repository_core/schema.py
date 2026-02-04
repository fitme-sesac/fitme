# app/chatbot/repository_core/schema.py
from __future__ import annotations

import functools
from typing import Optional

from app.core.database import get_db_connection

# -----------------------------
# Helpers: schema introspection
# -----------------------------

def _column_exists(table_name: str, column_name: str) -> bool:
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = %s
                  AND column_name = %s
                    LIMIT 1
                """,
                (table_name, column_name),
            )
            return cur.fetchone() is not None
    finally:
        conn.close()

def _relation_exists(relname: str, relkind: Optional[str] = None) -> bool:
    """
    relkind: 'r'(table), 'm'(materialized view), 'v'(view), None(any)
    """
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            if relkind:
                cur.execute(
                    """
                    SELECT 1
                    FROM pg_class c
                             JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public'
                      AND c.relname = %s
                      AND c.relkind = %s
                        LIMIT 1
                    """,
                    (relname, relkind),
                )
            else:
                cur.execute(
                    """
                    SELECT 1
                    FROM pg_class c
                             JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public'
                      AND c.relname = %s
                        LIMIT 1
                    """,
                    (relname,),
                )
            return cur.fetchone() is not None
    finally:
        conn.close()

def _has_pg_trgm() -> bool:
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT 1 FROM pg_extension WHERE extname='pg_trgm' LIMIT 1")
            return cur.fetchone() is not None
    finally:
        conn.close()
