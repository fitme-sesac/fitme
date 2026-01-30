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
import java.util.UUID

/**
 * [FitMe] Ad Click Performance Test Script
 * - Tests the CPC billing and click tracking logic.
 * - Generates unique clickKey for each request to simulate real traffic.
 */
@RunWith(GrinderRunner)
class AdClickBenchmark {

    public static GTest test
    public static HTTPRequest request
    
    // [Target Config]
    public static String HOST = "http://host.docker.internal:8081" 
    public static String ENDPOINT = "/api/v1/ad/clicks" 

    // [Test Data Config]
    public static long CAMPAIGN_ID = 1L // 실제 존재하는 캠페인 ID여야 함
    public static long MEMBER_ID = 1L   // 실제 존재하는 멤버 ID

    @BeforeProcess
    public static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = 10000
        test = new GTest(1, "Ad Click Benchmark")
        request = new HTTPRequest()
        
        // Content-Type: application/json 설정
        request.setHeaders([new NVPair("Content-Type", "application/json")] as NVPair[])
        
        grinder.logger.info("Setup complete. Target: " + HOST + ENDPOINT)
    }

    @BeforeThread
    public void beforeThread() {
        test.record(this, "test")
        grinder.statistics.delayReports=true
    }

    @Test
    public void test() {
        // Unique Click Key 생성 (중복 방지)
        String clickKey = UUID.randomUUID().toString()
        
        // JSON Body 구성
        String body = "{" +
            "\"campaignId\": " + CAMPAIGN_ID + "," +
            "\"memberId\": " + MEMBER_ID + "," +
            "\"clickKey\": \"" + clickKey + "\"" +
        "}"

        // POST 요청 전송
        HTTPResponse result = request.POST(HOST + ENDPOINT, body.getBytes())

        // 검증
        if (result.statusCode == 301 || result.statusCode == 302) {
            grinder.logger.warn("Redirected: " + result.statusCode)
        } else {
            // 200 OK
            assertThat(result.statusCode, is(200))
        }
    }
}
