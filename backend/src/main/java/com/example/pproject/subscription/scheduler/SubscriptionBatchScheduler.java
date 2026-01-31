package com.example.pproject.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job subscriptionBillingJob;

    // 매일 자정(00:00:00)에 실행
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "subscriptionBillingJob", lockAtLeastFor = "30s", lockAtMostFor = "10m")
    public void runBillingJob() {
        try {
            log.info("Starting subscription billing job at {}", LocalDateTime.now());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("datetime", LocalDateTime.now().toString()) // 매번 새로운 파라미터로 실행
                    .toJobParameters();

            jobLauncher.run(subscriptionBillingJob, jobParameters);

            log.info("Finished subscription billing job at {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to run subscription billing job", e);
        }
    }
}
