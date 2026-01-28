import asyncio
import aiohttp
import time
import os
import sys
import io
from dotenv import load_dotenv

# =============================================================================
# [터미널 출력 설정]
# 윈도우 환경 등에서 한글 깨짐 방지를 위해 UTF-8로 입출력을 강제 고정합니다.
# =============================================================================
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# =============================================================================
# [환경 설정(Environment Variables)]
# 프로젝트 루트의 .env.docker 파일을 읽어 데이터베이스 및 레디스 접속 정보를 설정합니다.
# =============================================================================
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.abspath(os.path.join(current_dir, "../.."))
env_docker_path = os.path.join(project_root, ".env.docker")

if os.path.exists(env_docker_path):
    load_dotenv(env_docker_path)
else:
    load_dotenv()

# [기본 테스트 설정]
# BASE_URL: 우리 자바 백엔드 서버 주소
# MEMBER_ID: 광고를 추천받을 대상 유저의 ID (이력서가 등록되어 있어야 함)
# TOTAL_REQUESTS: 총 몇 번의 클릭(요청)을 보낼지 결정
# CONCURRENT_REQUESTS: 한 번에 동시에 몇 명이 접속하는 것처럼 흉내낼지 (동시성 수준)
BASE_URL = "http://localhost:8081"
MEMBER_ID = 101
TOTAL_REQUESTS = 100
CONCURRENT_REQUESTS = 10

async def fetch_ad(session, url):
    """
    [단일 요청 수행 및 시간 측정]
    - 이 함수는 실제로 서버에 한 번 노크하고 응답이 올 때까지의 시간을 잽니다.
    - time.perf_counter()는 나노초 단위의 정밀한 시간 측정을 가능하게 합니다.
    """
    start = time.perf_counter() # 스톱워치 시작
    try:
        # 비동기 방식으로 서버에 GET 요청을 보냄
        async with session.get(url) as response:
            if response.status == 200:
                await response.json() # 응답 본문을 읽음 (완료 시점 확인용)
                return time.perf_counter() - start # 걸린 시간 반환 (초 단위)
            else:
                # 404, 500 등 에러 발생 시 로그 (필요시 주석 해제)
                print(f"Error: {response.status}")
                return None
    except Exception as e:
        # 네트워크 끊김 등의 예외 발생 시 실패
        print(f"Exception: {e}")
        return None

async def run_benchmark(name, endpoint):
    """
    [특정 버전(V1/V2) 벤치마크 수행]
    - 특정 URL(엔드포인트)에 대해 부하 테스트를 수행하고 통계를 냅니다.
    """
    url = f"{BASE_URL}{endpoint}?memberId={MEMBER_ID}"
    
    # [세마포어(Semaphore)란?]
    # 동시 요청 수를 제한하는 안전장치입니다.
    # 100개를 동시에 보내라고 해도, 딱 'CONCURRENT_REQUESTS(10개)'씩만 
    # 허용해서 서버가 한 번에 처리할 수 있는 부하의 질을 조절합니다.
    sem = asyncio.Semaphore(CONCURRENT_REQUESTS)
    
    async def limited_fetch(session):
        # 세마포어 허락을 받아야만 서버에 요청을 보낼 수 있음
        async with sem:
            return await fetch_ad(session, url)

    async with aiohttp.ClientSession() as session:
        print(f"\n[시작] [{name}] 벤치마크 시작 (대상: {endpoint})")
        print(f"   - 총 요청: {TOTAL_REQUESTS}건, 동시성: {CONCURRENT_REQUESTS}")
        
        # [Warm-up 단계]
        # 서버와 DB 커넥션 풀이 처음엔 '잠들어' 있을 수 있습니다.
        # 공정한 측정을 위해 5번 미리 요청을 보내 '예열'을 시킵니다.
        for _ in range(5):
            await fetch_ad(session, url)
        
        # [메인 측정 시작]
        start_time = time.perf_counter() # 전체 테스트 시작 시간
        
        # TOTAL_REQUESTS 만큼의 비동기 작업(Tasks) 모음을 생성
        tasks = [limited_fetch(session) for _ in range(TOTAL_REQUESTS)]
        
        # gather: 100개의 작업을 동시에 마구 실행하고 결과를 한데 모음
        latencies = await asyncio.gather(*tasks)
        
        total_duration = time.perf_counter() - start_time # 전체 소요 시간
        
        # [결과 분석]
        valid_latencies = [l for l in latencies if l is not None] # 성공한 데이터만 추출
        success_count = len(valid_latencies)
        fail_count = TOTAL_REQUESTS - success_count
        
        if success_count > 0:
            avg_latency = (sum(valid_latencies) / success_count) * 1000
            tps = success_count / total_duration
            
            # [Quality Check] 결과의 품질(평균 하이브리드 점수/유사도) 측정
            total_quality_score = 0.0
            total_items = 0
            
            # 각 요청의 응답 본문에서 점수 추출 (fetch_ad 함수 수정 필요)
            # 여기서는 fetch_ad가 latency만 반환하므로, 샘플링을 위해 별도로 1회 요청하여 품질 측정
            try:
                async with session.get(url) as response:
                    if response.status == 200:
                        data = await response.json()
                        for item in data:
                            # V3는 hybridScore, V1/V2는 similarity가 있을 수 있음
                            score = item.get('hybridScore', item.get('similarity', 0.0))
                            if score is None: score = 0.0
                            total_quality_score += float(score)
                            total_items += 1
            except:
                pass
            
            avg_quality = (total_quality_score / total_items) if total_items > 0 else 0.0

            print(f"[결과]:")
            print(f"   - 평균 응답 시간: {avg_latency:.2f} ms")
            print(f"   - 초당 처리량 (TPS): {tps:.2f}")
            print(f"   - 평균 품질 점수 (샘플링): {avg_quality:.4f} (높을수록 좋음)")
            print(f"   - 성공: {success_count}건, 실패: {fail_count}건")
            
            return avg_latency
        else:
            print(f"[실패]: 모든 요청이 실패했습니다. 서버가 실행 중인지 확인하세요.")
            return None

async def main():
    """
    [전체 테스트 시나리오]
    1. V1 테스트를 수행
    2. V2 테스트를 수행
    3. 둘을 비교 분석하여 성적표 출력
    """
    print("="*60)
    print("      광고 시스템 V1 vs V2 성능 비교 벤치마크")
    print("="*60)
    
    # 1. V1 (RDB Direct) 테스트: DB가 직접 필터링하고 모든 걸 계산하는 방식
    v1_latency = await run_benchmark("V1: Postgres Direct", "/api/v1/ad/serve/match")
    
    print("\n" + "-"*40)
    # 2. V2 Legacy (Redis Pre-Filtering) 테스트
    v2_latency = await run_benchmark("V2: Redis Filter (Legacy)", "/api/v2/ad/serve/match/v2-legacy")
    
    print("\n" + "-"*40)

    # 3. V3 (Hybrid Architecture) 테스트
    # URL은 기존 V2 엔드포인트(match)를 사용하되, 내부 로직은 V3로 업그레이드됨
    v3_latency = await run_benchmark("V3: Hybrid (Recall -> Guard)", "/api/v2/ad/serve/match")
    
    # [최종 비교 로직]
    print("\n" + "="*60)
    print(f"[분석] 최종 아키텍처 비교 리포트")
    
    if v1_latency and v2_latency and v3_latency:
        # V1 vs V3 비교
        speedup_v3 = v1_latency / v3_latency
        print(f"\n1. [V1 vs V3] (최종 진화)")
        if speedup_v3 > 1:
            print(f"   - V3가 V1보다 약 {speedup_v3:.1f}배 빠릅니다.")
        else:
            print(f"   - V3가 V1보다 약 {1/speedup_v3:.1f}배 느립니다.")
            
        # V2 vs V3 비교
        speedup_v2_v3 = v2_latency / v3_latency
        print(f"\n2. [V2 vs V3] (최적화 효과)")
        if speedup_v2_v3 > 1:
            print(f"   - V3가 V2(Legacy)보다 약 {speedup_v2_v3:.1f}배 빠릅니다.")
        else:
            print(f"   - V3가 V2(Legacy)보다 약 {1/speedup_v2_v3:.1f}배 느립니다.")
            
    print("="*60)

if __name__ == "__main__":
    # 비동기 루프 실행 (Python 3.7+ 표준 방식)
    asyncio.run(main())
