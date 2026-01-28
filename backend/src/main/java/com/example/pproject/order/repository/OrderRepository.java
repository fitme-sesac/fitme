package com.example.pproject.order.repository;

import com.example.pproject.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Orders, Long> {
    Optional<Orders> findByOrderUid(UUID orderUid);
    Optional<Orders> findByIdempotencyKey(String idempotencyKey);
}
