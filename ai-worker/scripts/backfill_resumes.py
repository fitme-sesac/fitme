import sys
import os
import asyncio
import io
import logging

sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.database import get_db_connection
from scripts.batch_process_resume import process_resume_by_id

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger("ResumeBackfill")

async def main(limit: int = None):
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT resume_id FROM resume ORDER BY resume_id DESC") # 최신순
            ids = [row[0] for row in cur.fetchall()]
        
        if limit:
            logger.info(f"⚠️ Limit applied: Processing top {limit} records.")
            ids = ids[:limit]
        
        logger.info(f"📋 Found {len(ids)} resumes to process.")
        
        total = len(ids)
        for idx, resume_id in enumerate(ids):
            logger.info(f"🔄 [{idx+1}/{total}] Processing Resume ID: {resume_id}")
            try:
                await process_resume_by_id(resume_id)
            except Exception as e:
                logger.error(f"❌ Failed Resume {resume_id}: {e}")
                
            await asyncio.sleep(0.5) # Rate limit protection
            
        logger.info("🏁 All resumes processed.")
        
    except Exception as e:
        logger.error(f"Script Error: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    limit = None
    if len(sys.argv) > 1 and sys.argv[1].startswith("--limit="):
        try:
            limit = int(sys.argv[1].split("=")[1])
        except ValueError:
            print("Invalid limit value")
            sys.exit(1)
            
    asyncio.run(main(limit))
