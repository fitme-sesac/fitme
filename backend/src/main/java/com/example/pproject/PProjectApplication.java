package com.example.pproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync // 비동기 처리 활성화
@EnableScheduling // 스케줄링 활성화 (아웃박스 이벤트 처리용)
@SpringBootApplication
public class PProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(PProjectApplication.class, args);
	}

}
