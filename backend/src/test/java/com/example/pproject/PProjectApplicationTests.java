package com.example.pproject;

import com.example.pproject.employer.controller.EmployerController;
import com.example.pproject.employer.service.EmployerService;
import com.example.pproject.resume.controller.ResumeController;
import com.example.pproject.resume.service.ResumeService;
import com.example.pproject.user.controller.UserController;
import com.example.pproject.user.service.UserService;
import com.example.pproject.job.service.JobService;
import com.example.pproject.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 애플리케이션 컴파일 오류 및 빈 주입 오류 체크 테스트
 *
 * 이 테스트는 다음을 확인합니다:
 * 1. 애플리케이션 컨텍스트 로딩 (컴파일 오류, 설정 오류)
 * 2. 주요 서비스 빈 주입 (의존성 주입 오류)
 * 3. 주요 컨트롤러 빈 주입 (컴포넌트 스캔 오류)
 */
@SpringBootTest
class PProjectApplicationTests {

    @Autowired(required = false)
    private EmployerService employerService;

    @Autowired(required = false)
    private ResumeService resumeService;

    @Autowired(required = false)
    private UserService userService;

    @Autowired(required = false)
    private JobService jobService;

    @Autowired(required = false)
    private WalletService walletService;

    @Autowired(required = false)
    private EmployerController employerController;

    @Autowired(required = false)
    private ResumeController resumeController;

    @Autowired(required = false)
    private UserController userController;

    /**
     * 기본 컨텍스트 로딩 테스트
     * - 컴파일 오류 체크
     * - 설정 파일 오류 체크
     * - 데이터베이스 연결 설정 체크
     */
    @Test
    void contextLoads() {
        // 컨텍스트가 성공적으로 로드되면 테스트 통과
    }

    /**
     * 주요 서비스 빈 주입 테스트
     * - 의존성 주입 오류 체크
     * - 순환 참조 오류 체크
     * - 빈 생성 오류 체크
     */
    @Test
    void servicesShouldBeInjected() {
        // 주요 서비스들이 제대로 주입되었는지 확인
        assertThat(employerService).as("EmployerService 빈 주입 확인").isNotNull();
        assertThat(resumeService).as("ResumeService 빈 주입 확인").isNotNull();
        assertThat(userService).as("UserService 빈 주입 확인").isNotNull();
        assertThat(jobService).as("JobService 빈 주입 확인").isNotNull();
        assertThat(walletService).as("WalletService 빈 주입 확인").isNotNull();
    }

    /**
     * 주요 컨트롤러 빈 주입 테스트
     * - 컴포넌트 스캔 오류 체크
     * - 컨트롤러 등록 오류 체크
     */
    @Test
    void controllersShouldBeInjected() {
        // 주요 컨트롤러들이 제대로 주입되었는지 확인
        assertThat(employerController).as("EmployerController 빈 주입 확인").isNotNull();
        assertThat(resumeController).as("ResumeController 빈 주입 확인").isNotNull();
        assertThat(userController).as("UserController 빈 주입 확인").isNotNull();
    }
}