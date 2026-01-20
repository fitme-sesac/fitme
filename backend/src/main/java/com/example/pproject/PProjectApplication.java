package com.example.pproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync // 비동기 처리 활성화
@SpringBootApplication
public class PProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(PProjectApplication.class, args);
	}

}
