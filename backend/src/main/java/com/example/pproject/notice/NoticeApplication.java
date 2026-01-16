package com.example.pproject.notice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notice 모듈 독립 실행 애플리케이션
 * 공지사항 및 정책동의서 관리 기능을 포함합니다.
 */

public class NoticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoticeApplication.class, args);
    }
}
