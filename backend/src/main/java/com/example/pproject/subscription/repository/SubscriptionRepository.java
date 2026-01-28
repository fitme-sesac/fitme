package com.example.pproject.subscription.repository;

import com.example.pproject.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByEmployer_Id(Long employerId);
    List<Subscription> findAllByEmployer_Id(Long employerId);
}
