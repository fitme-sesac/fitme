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

/**
 * [FitMe 광고 시스템] 멱등성(Idempotency) 검증 테스트 스크립트
 * 
 * ■ 테스트 목적:
 *    - 사용자가 실수로 따닥(Double Click)하거나 네트워크 지연으로 재전송된 요청에 대해
 *      서버가 중복 과금을 하지 않고 방어하는지(멱등성) 검증.
 * 
 * ■ 동작 방식:
 *    - 1회 테스트(`test()`) 내에서 동일한 ClickKey를 가진 요청을 2번 연속 발송.
 *    - 1번째 요청: 성공(200 OK) 기대.
 *    - 2번째 요청: 성공(200, 내부 로직 Skip) 또는 실패(409 Conflict) 기대.
 *    - 500 에러나 예외가 발생하면 테스트 실패.
 */
@RunWith(GrinderRunner)
class IdempotencyTest {

    public static GTest test
    public static HTTPRequest request
    
    // [설정] 타겟 서버
    public static String HOST = "http://127.0.0.1:8081" 
    public static String ENDPOINT = "/api/v1/ad/clicks"

    public static long CAMPAIGN_ID = 1L
    public static long MEMBER_ID = 1L

    @BeforeProcess
    public static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = 10000
        test = new GTest(1, "Idempotency Check (Double Submit)")
        request = new HTTPRequest()
        request.addHeader("Content-Type", "application/json")
        grinder.logger.info("[Setup] Idempotency Test Target: " + HOST + ENDPOINT)
    }

    @BeforeThread
    public void beforeThread() {
        test.record(this, "test")
        grinder.statistics.delayReports=true
    }

    @Test
    public void test() {
        // 1. 이번 회차에 사용할 고유 키 생성
        String clickKey = UUID.randomUUID().toString()
        
        String body = """{
            "campaignId": ${CAMPAIGN_ID},
            "memberId": ${MEMBER_ID},
            "clickKey": "${clickKey}"
        }"""
        
        // 2. [Step A] 첫 번째 요청 (First Submit) -> 반드시 성공해야 함
        HTTPResponse res1 = request.POST(HOST + ENDPOINT, body.getBytes("UTF-8"))
        
        if (res1.statusCode != 200) {
            grinder.logger.error("[FAIL] First request failed with status: " + res1.statusCode)
            fail("First request must succeed")
            return
        }

        // 3. [Step B] 두 번째 요청 (Duplicate Submit) -> 방어 로직 작동해야 함
        // 동일한 body, 동일한 clickKey로 재요청
        HTTPResponse res2 = request.POST(HOST + ENDPOINT, body.getBytes("UTF-8"))

        // 4. 응답 검증
        // Case A: 명시적 거부 (409 Conflict)
        if (res2.statusCode == 409 || res2.statusCode == 400) {
             // 정상 동작: 중복 요청임을 알고 거부함
             // grinder.logger.info("[PASS] Duplicate rejected (409/400)")
        } 
        // Case B: 묵시적 성공 (200 OK)
        // 클라이언트에게는 성공처럼 보이지만, 서버 내부에서는 DB unique constraint 등으로 인해 과금 로직이 Skip 되어야 함.
        // (nGrinder에서는 내부 로직까지 확인 불가능하므로 status 200이면 일단 Pass로 간주)
        else if (res2.statusCode == 200) {
             // grinder.logger.info("[PASS] Duplicate returned 200 (Idempotent response)")
        } 
        // Case C: 서버 오류 (500)
        else {
             grinder.logger.warn("[WARN] Unexpected status for duplicate: " + res2.statusCode)
             fail("Server Error on duplicate request")
        }
    }
}
