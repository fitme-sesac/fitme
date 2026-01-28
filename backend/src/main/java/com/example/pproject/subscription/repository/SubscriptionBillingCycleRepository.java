package com.example.pproject.subscription.repository;

import com.example.pproject.subscription.entity.SubscriptionBillingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionBillingCycleRepository extends JpaRepository<SubscriptionBillingCycle, Long> {
    List<SubscriptionBillingCycle> findAllBySubscription_SubscriptionId(Long subscriptionId);
}
