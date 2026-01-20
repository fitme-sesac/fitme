package com.example.pproject.notice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notice 모듈 독립 실행 애플리케이션
 */
// ▼▼▼ [중요] 이 줄이 없으면 스프링이 켜지지 않습니다! ▼▼▼
// scanBasePackages를 넣어줘야 다른 패키지의 설정(Security, DB)까지 다 불러옵니다.
@SpringBootApplication(scanBasePackages = "com.example.pproject")
public class NoticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoticeApplication.class, args);
    }
}