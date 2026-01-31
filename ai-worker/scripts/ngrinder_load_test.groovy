import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.*
import static org.hamcrest.Matchers.*
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPPluginControl
import net.grinder.script.GTest
import net.grinder.script.Grinder
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Test
import org.junit.runner.RunWith
import HTTPClient.HTTPResponse
import HTTPClient.NVPair

/**
 * [FitMe 광고 시스템] 성능 부하 테스트 스크립트 (조회 API)
 * 
 * ■ 테스트 목적:
 *    - 광고 목록 조회(/api/v2/ad/serve/match) API의 TPS 및 응답 속도 측정
 *    - V1, V2, V3 아키텍처 간 성능 비교 검증
 * 
 * ■ 사용 방법:
 *    - 아래 'ENDPOINT' 변수의 주석을 해제하여 V1, V2, V3 중 원하는 시나리오 선택
 *    - nGrinder Agent가 Docker 컨테이너인 경우 HOST 주소를 'host.docker.internal'로 변경
 * 
 * ■ 주요 지표:
 *    - TPS (초당 처리 건수): 최소 30 이상 권장
 *    - MTT (평균 응답 시간): 300ms 이내 권장
 */
@RunWith(GrinderRunner)
class AdServeBenchmark {

    public static GTest test
    public static HTTPRequest request
    
    // [설정 1] 타겟 서버 주소
    // 로컬 실행 시: "http://127.0.0.1:8081"
    // 도커 실행 시: "http://host.docker.internal:8081" (Agent가 컨테이너 내부일 경우)
    public static String HOST = "http://127.0.0.1:8081" 

    // [설정 2] 테스트할 시나리오 선택 (주석 해제/설정)
    // -------------------------------------------------------------
    // [Case 1] V3 하이브리드 (최종 권장): Recall(DB) + Guard(Redis) -> 가장 안정적
    public static String ENDPOINT = "/api/v2/ad/serve/match" 
    
    // [Case 2] V2 레거시 (비추천): Redis ID 전체 필터링 -> 30,000개 이상 시 장애 발생
    // public static String ENDPOINT = "/api/v2/ad/serve/match/v2-legacy"

    // [Case 3] V1 순수 DB (비추천): 예산 체크 미흡, 품질 낮음
    // public static String ENDPOINT = "/api/v1/ad/serve/match"
    // -------------------------------------------------------------

    public static long MEMBER_ID = 1L // 테스트할 사용자 ID (DB에 존재하는 ID여야 함)

    @BeforeProcess
    public static void beforeProcess() {
        // [설정 3] 타임아웃 설정 (ms): 10초 넘으면 에러 처리
        HTTPPluginControl.getConnectionDefaults().timeout = 10000
        test = new GTest(1, "Ad Serve Benchmark")
        request = new HTTPRequest()
        grinder.logger.info("[Setup] Target: " + HOST + ENDPOINT)
    }

    @BeforeThread
    public void beforeThread() {
        test.record(this, "test")
        grinder.statistics.delayReports=true
    }

    @Test
    public void test() {
        // 1. 요청 파라미터 구성
        NVPair[] params = [
            new NVPair("memberId", String.valueOf(MEMBER_ID)),
            new NVPair("limit", "5") // 한 번에 가져올 광고 개수
        ] as NVPair[]
        
        // 2. GET 요청 전송
        HTTPResponse result = request.GET(HOST + ENDPOINT, params)

        // 3. 응답 검증
        if (result.statusCode == 301 || result.statusCode == 302) {
            grinder.logger.warn("Warning: Redirected (" + result.statusCode + ")")
        } else {
            // 200 OK가 아니면 실패 처리됨
            assertThat(result.statusCode, is(200))
        }
    }
}
