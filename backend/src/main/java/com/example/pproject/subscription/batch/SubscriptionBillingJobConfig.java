package com.example.pproject.subscription.batch;

import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.subscription.entity.Subscription;
import com.example.pproject.subscription.repository.SubscriptionRepository;
import com.example.pproject.subscription.service.SubscriptionBillingCycleService;
import com.example.pproject.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SubscriptionBillingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionBillingCycleService billingCycleService;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job subscriptionBillingJob() {
        return new JobBuilder("subscriptionBillingJob", jobRepository)
                .start(subscriptionBillingStep())
                .build();
    }

    @Bean
    public Step subscriptionBillingStep() {
        return new StepBuilder("subscriptionBillingStep", jobRepository)
                .<Subscription, Subscription>chunk(CHUNK_SIZE, transactionManager)
                .reader(subscriptionBillingReader())
                .processor(subscriptionBillingProcessor())
                .writer(subscriptionBillingWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<Subscription> subscriptionBillingReader() {
        return new RepositoryItemReaderBuilder<Subscription>()
                .name("subscriptionBillingReader")
                .repository(subscriptionRepository)
                .methodName("findAllByStatusAndNextBillingAtLessThanEqual")
                .arguments(SubscriptionStatus.ACTIVE, Instant.now())
                .pageSize(CHUNK_SIZE)
                .sorts(Collections.singletonMap("subscriptionId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Subscription, Subscription> subscriptionBillingProcessor() {
        return subscription -> {
            try {
                log.info("Processing subscription id: {}", subscription.getSubscriptionId());

                // 1. 결제 가능 상태 확인
                subscriptionService.validateBillableState(subscription.getSubscriptionId());

                // 2. 상품 변경 예약 적용 (있다면)
                if (subscription.isChangeScheduled()) {
                    subscriptionService.applyScheduledProductChange(subscription.getSubscriptionId());
                }

                // 3. 월별 정기 결제 처리 (결제 + 사이클 생성 + 크레딧 지급)
                billingCycleService.processMonthlyPayment(subscription.getSubscriptionId());

                // 4. 다음 결제일 스케줄링
                subscriptionService.scheduleNextBilling(subscription.getSubscriptionId());

                return subscription;
            } catch (Exception e) {
                log.error("Failed to process subscription id: {}", subscription.getSubscriptionId(), e);
                return null; // 실패한 항목은 건너뜀
            }
        };
    }

    @Bean
    public ItemWriter<Subscription> subscriptionBillingWriter() {
        return items -> {
            for (Subscription subscription : items) {
                log.info("Successfully processed subscription id: {}", subscription.getSubscriptionId());
            }
        };
    }
}
