package com.example.pproject.subscription.repository;

import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.subscription.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByEmployer_Id(Long employerId);

    List<Subscription> findAllByEmployer_Id(Long employerId);

    // 배치용 조회 메서드 (Pageable 필수)
    Page<Subscription> findAllByStatusAndNextBillingAtLessThanEqual(
            SubscriptionStatus status,
            Instant nextBillingAt,
            Pageable pageable);

    // 활성 구독 확인 (status가 ACTIVE이고, endedAt이 없거나 아직 종료되지 않은 구독)
    @Query("SELECT s FROM Subscription s WHERE s.employer.id = :employerId AND s.status = :status AND (s.endedAt IS NULL OR s.endedAt > :now)")
    Optional<Subscription> findActiveByEmployerId(@Param("employerId") Long employerId,
            @Param("status") SubscriptionStatus status, @Param("now") Instant now);

    // 활성 구독 존재 여부
    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.employer.id = :employerId AND s.status = :status AND (s.endedAt IS NULL OR s.endedAt > :now)")
    boolean hasActiveSubscription(@Param("employerId") Long employerId, @Param("status") SubscriptionStatus status,
            @Param("now") Instant now);

    // 특정 상태의 구독 수 카운트 (활성 기업 수)
    long countByStatus(SubscriptionStatus status);

    // 실제 활성 구독 수 카운트 (status가 ACTIVE이고, 아직 종료되지 않은 구독)
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = :status AND (s.endedAt IS NULL OR s.endedAt > :now)")
    long countActiveSubscriptions(@Param("status") SubscriptionStatus status, @Param("now") Instant now);
}
