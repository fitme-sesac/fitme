package com.example.pproject.faq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * FAQ 모듈 독립 실행 애플리케이션
 * 이 애플리케이션은 FAQ 기능만 포함하며, 다른 모듈과 독립적으로 동작합니다.
 */
public class FaqApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaqApplication.class, args);
    }
}
