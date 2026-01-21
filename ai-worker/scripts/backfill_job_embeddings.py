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

async def backfill_job_embeddings(only_null=True):
    """
    모든 채용 공고에 대해 임베딩을 생성 및 업데이트합니다.
    """
    logger.info(f"🚀 [Backfill Start] Job Embedding Backfill initiated. (only_null={only_null})")
    
    # 1. 대상 Job ID 가져오기 (DB IO is synchronous in this repo, so it's fine)
    target_ids = job_repo.get_target_job_ids(only_null=only_null)
    
    if not target_ids:
        logger.info("✨ [Done] 대상 공고가 없습니다. 모든 공고가 이미 임베딩을 가지고 있거나 공고가 없습니다.")
        return

    logger.info(f"📋 [Target Found] 총 {len(target_ids)} 개의 공고를 처리합니다.")
    
    success_count = 0
    fail_count = 0
    
    for index, job_id in enumerate(target_ids, 1):
        try:
            logger.info(f"🔄 [{index}/{len(target_ids)}] Processing Job ID: {job_id}...")
            
            # 임베딩 생성 및 DB 업데이트 (Service 메서드 재사용)
            # generate_and_update_embedding is async
            await job_service.generate_and_update_embedding(job_id)
            
            success_count += 1
            
            # OpenAI Rate Limit 예방을 위한 아주 짧은 대기 (await time.sleep is not valid, use asyncio.sleep)
            await asyncio.sleep(0.5)
            
        except Exception as e:
            fail_count += 1
            logger.error(f"❌ [Error] Failed to process Job ID {job_id}: {e}")
            # 배치는 멈추지 않고 다음으로 넘어감
            continue
            
    logger.info("=" * 60)
    logger.info(f"🏁 [Backfill Finished] Total: {len(target_ids)}, Success: {success_count}, Failed: {fail_count}")

if __name__ == "__main__":
    # 실행 인자 확인 (e.g., python backfill.py --all)
    # --all 옵션을 주면 이미 있는 것도 다시 다 덮어씁니다.
    run_only_null = True
    if len(sys.argv) > 1 and sys.argv[1] == "--all":
        run_only_null = False
        
    asyncio.run(backfill_job_embeddings(only_null=run_only_null))
