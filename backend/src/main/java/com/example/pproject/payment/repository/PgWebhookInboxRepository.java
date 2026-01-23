package com.example.pproject.payment.repository;

import com.example.pproject.payment.entity.PgWebhookInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PgWebhookInboxRepository extends JpaRepository<PgWebhookInbox, Long> {
    // 현재는 기본 CRUD만 사용
}
