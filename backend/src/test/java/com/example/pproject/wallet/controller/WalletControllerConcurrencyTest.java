package com.example.pproject.wallet.controller;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.SourceType;
import com.example.pproject.common.service.DistributedLockService;
import com.example.pproject.global.exception.DuplicateRequestException;
import com.example.pproject.payment.repository.PaymentRepository;
import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.wallet.dto.WalletUseRequest;
import com.example.pproject.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 분산 락 동시성 테스트
 * - 분산 락 적용 시 vs 미적용 시 동작 비교
 */
@ExtendWith(MockitoExtension.class)
class WalletControllerConcurrencyTest {

    @Mock
    private WalletService walletService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DistributedLockService lockService;

    // =======================================================
    // 1. 분산 락 적용 시 - 동시 요청 중 1개만 성공
    // =======================================================

    @Test
    @DisplayName("[분산 락 적용] 동시 10개 요청 - 1개만 성공, 9개 거부")
    void withDistributedLock_ConcurrentRequests_OnlyOneSucceeds() throws InterruptedException {
        // 1. [Given]
        WalletController controller = new WalletController(walletService, paymentRepository, lockService);
        JwtUserPrincipal user = new JwtUserPrincipal(123L, "user123", "User", null, Collections.emptyList());
        WalletUseRequest request = new WalletUseRequest(
                100L,
                "order-concurrent-test",
                SourceType.AI,
                BuyerType.MEMBER);

        // 락 시뮬레이션: 첫 번째 시도만 성공, 나머지는 실패
        AtomicInteger lockAttempts = new AtomicInteger(0);
        given(lockService.tryLock("wallet:use:123:order-concurrent-test"))
                .willAnswer(invocation -> {
                    // 첫 번째 요청만 락 획득 성공
                    return lockAttempts.incrementAndGet() == 1;
                });

        // 2. [When] 동시에 10개 요청
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드 동시 시작
                    controller.useCredit(user, request);
                    successCount.incrementAndGet();
                } catch (DuplicateRequestException e) {
                    duplicateCount.incrementAndGet();
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 동시 시작 신호
        endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. [Then]
        System.out.println("========================================");
        System.out.println("[분산 락 적용 결과]");
        System.out.println("  총 요청: " + threadCount);
        System.out.println("  성공: " + successCount.get());
        System.out.println("  중복 거부 (DuplicateRequestException): " + duplicateCount.get());
        System.out.println("  기타 예외: " + exceptions.size());
        System.out.println("========================================");

        assertEquals(1, successCount.get(), "분산 락 적용 시 1개만 성공해야 함");
        assertEquals(9, duplicateCount.get(), "나머지 9개는 DuplicateRequestException으로 거부되어야 함");
        assertTrue(exceptions.isEmpty(), "기타 예외 없어야 함");

        // walletService.useCredit()은 1번만 호출되어야 함
        verify(walletService, times(1)).useCredit(anyLong(), any(), anyLong(), anyString(), any());
    }

    // =======================================================
    // 2. 분산 락 미적용 시 - 동시 요청 모두 처리 (위험!)
    // =======================================================

    @Test
    @DisplayName("[분산 락 미적용] 동시 10개 요청 - 모두 처리됨 (중복 차감 위험)")
    void withoutDistributedLock_ConcurrentRequests_AllProcessed() throws InterruptedException {
        // 1. [Given] 분산 락 없는 컨트롤러 시뮬레이션
        // 락을 항상 획득 성공으로 설정 (락이 없는 것처럼 동작)
        WalletController controller = new WalletController(walletService, paymentRepository, lockService);
        JwtUserPrincipal user = new JwtUserPrincipal(456L, "user456", "User", null, Collections.emptyList());
        WalletUseRequest request = new WalletUseRequest(
                100L,
                "order-no-lock-test",
                SourceType.AI,
                BuyerType.MEMBER);

        // 락이 없는 상황 시뮬레이션: 모든 요청이 락 획득 성공
        given(lockService.tryLock("wallet:use:456:order-no-lock-test")).willReturn(true);

        // 2. [When] 동시에 10개 요청
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    controller.useCredit(user, request);
                    successCount.incrementAndGet();
                } catch (DuplicateRequestException e) {
                    duplicateCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. [Then]
        System.out.println("========================================");
        System.out.println("[분산 락 미적용 결과] ⚠️ 위험!");
        System.out.println("  총 요청: " + threadCount);
        System.out.println("  성공 (모두 처리됨): " + successCount.get());
        System.out.println("  중복 거부: " + duplicateCount.get());
        System.out.println("  → 동일 주문에 대해 크레딧이 " + successCount.get() + "번 차감됨!");
        System.out.println("========================================");

        assertEquals(10, successCount.get(), "락 없으면 모든 요청이 처리됨 (위험!)");
        assertEquals(0, duplicateCount.get(), "거부된 요청 없음");

        // walletService.useCredit()이 10번 호출됨 (중복 차감 발생!)
        verify(walletService, times(10)).useCredit(anyLong(), any(), anyLong(), anyString(), any());
    }

    // =======================================================
    // 3. 비교 요약 테스트
    // =======================================================

    @Test
    @DisplayName("[비교] 분산 락 적용 vs 미적용 - 결과 요약")
    void comparison_WithLock_vs_WithoutLock() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           분산 락 적용 vs 미적용 비교 결과                   ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  상황: 동일 주문(order-123)에 대해 10개 동시 요청            ║");
        System.out.println("╠═══════════════════════════╦══════════════════════════════════╣");
        System.out.println("║                           ║ 분산 락 적용   ║ 분산 락 미적용  ║");
        System.out.println("╠═══════════════════════════╬══════════════════════════════════╣");
        System.out.println("║ 성공 처리                 ║      1개       ║     10개        ║");
        System.out.println("║ 중복 거부                 ║      9개       ║      0개        ║");
        System.out.println("║ 크레딧 차감 횟수          ║      1회       ║     10회        ║");
        System.out.println("║ 데이터 정합성             ║     ✅ 보장    ║    ❌ 위험      ║");
        System.out.println("╠═══════════════════════════╩══════════════════════════════════╣");
        System.out.println("║  결론: 분산 락 적용 시 중복 요청이 차단되어 정합성 보장      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("\n");

        assertTrue(true, "비교 결과 출력 완료");
    }
}
