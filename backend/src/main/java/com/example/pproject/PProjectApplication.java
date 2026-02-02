package com.example.pproject;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync      // 비동기 처리 활성화
@EnableScheduling // 스케줄러 활성화 (OutboxEventConsumer용)
@EnableBatchProcessing // Spring Batch 활성화
@SpringBootApplication
public class PProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(PProjectApplication.class, args);
    }

}
