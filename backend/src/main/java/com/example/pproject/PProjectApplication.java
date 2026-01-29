package com.example.pproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

// @EnableAsync // 비동기 처리 활성화
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.pproject")
public class PProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(PProjectApplication.class, args);
	}

}
