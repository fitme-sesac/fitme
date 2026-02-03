package com.example.pproject.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * AI 작업을 전담하는 스레드 풀
     * - AI API 호출은 응답이 느리므로(30초~1분), 전용 스레드풀을 사용하여 다른 비동기 작업(메일 등)에 영향을 주지 않도록 함
     */
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본적으로 5개 스레드 유지
        executor.setMaxPoolSize(20); // 최대 20개까지 확장
        executor.setQueueCapacity(50); // 대기열 50개
        executor.setThreadNamePrefix("AiWorker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // 종료 시 60초 대기 (중요: 작업 중단 방지)
        executor.initialize();
        return executor;
    }
}
