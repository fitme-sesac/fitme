package com.example.pproject.subscription.repository;

import com.example.pproject.Constant.SubscriptionStatus;
import com.example.pproject.subscription.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
            Pageable pageable
    );
}
