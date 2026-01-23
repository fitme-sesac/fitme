# app/core/redis.py
from __future__ import annotations

from typing import Optional
import redis.asyncio as redis

from app.core.config import settings

_redis: Optional[redis.Redis] = None


def get_redis() -> redis.Redis:
    global _redis
    if _redis is None:
        _redis = redis.Redis(
            host=settings.REDIS_HOST,
            port=int(settings.REDIS_PORT),
            password=settings.REDIS_PASSWORD,
            encoding="utf-8",
            decode_responses=True,
        )
    return _redis


async def close_redis() -> None:
    global _redis
    if _redis is not None:
        await _redis.close()
        await _redis.connection_pool.disconnect()
        _redis = None
