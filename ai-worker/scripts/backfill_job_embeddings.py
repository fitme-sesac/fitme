import asyncio
import sys
import os
import io
import time
import logging

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# 프로젝트 루트 경로 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.jobpostings.repository import job_repo
from app.jobpostings.services import job_service

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("BackfillScript")

async def backfill_job_embeddings(only_null=True, explicit_range=None):
    """
    모든 채용 공고에 대해 임베딩을 생성 및 업데이트합니다.
    explicit_range: (start_id, end_id) 튜플. 지정 시 해당 범위만 강제 실행.
    """
    if explicit_range:
        start, end = explicit_range
        logger.info(f"🚀 [Backfill Start] Range Mode: ID {start} ~ {end}")
        target_ids = list(range(start, end + 1))
    else:
        logger.info(f"🚀 [Backfill Start] Auto Mode (only_null={only_null})")
        # 1. 대상 Job ID 가져오기 (DB IO is synchronous in this repo, so it's fine)
        target_ids = job_repo.get_target_job_ids(only_null=only_null)
    
    if not target_ids:
        logger.info("✨ [Done] 대상 공고가 없습니다.")
        return

    logger.info(f"📋 [Target Found] 총 {len(target_ids)} 개의 공고를 처리합니다.")
    
    success_count = 0
    fail_count = 0
    
    for index, job_id in enumerate(target_ids, 1):
        try:
            logger.info(f"🔄 [{index}/{len(target_ids)}] Processing Job ID: {job_id}...")
            
            # 임베딩 생성 및 DB 업데이트 (Service 메서드 재사용)
            await job_service.generate_and_update_embedding(job_id)
            
            success_count += 1
            await asyncio.sleep(0.5)
            
        except Exception as e:
            fail_count += 1
            # 범위 실행 시 없는 ID는 에러가 아니라 그냥 넘어가는 게 자연스러우므로 warning 처리
            if "Job Posting not found" in str(e):
                logger.warning(f"⚠️ Job ID {job_id} not found in DB.")
            else:
                logger.error(f"❌ [Error] Failed to process Job ID {job_id}: {e}")
            continue
            
    logger.info("=" * 60)
    logger.info(f"🏁 [Backfill Finished] Total: {len(target_ids)}, Success: {success_count}, Failed: {fail_count}")

if __name__ == "__main__":
    # 실행 인자 확인
    # 1. Range Mode: python backfill.py 100 200
    # 2. All Mode: python backfill.py --all
    # 3. Default (Null only): python backfill.py
    
    run_only_null = True
    explicit_range = None
    
    args = sys.argv[1:]
    
    if len(args) == 2 and args[0].isdigit() and args[1].isdigit():
        explicit_range = (int(args[0]), int(args[1]))
    elif len(args) > 0 and args[0] == "--all":
        run_only_null = False
        
    asyncio.run(backfill_job_embeddings(only_null=run_only_null, explicit_range=explicit_range))
