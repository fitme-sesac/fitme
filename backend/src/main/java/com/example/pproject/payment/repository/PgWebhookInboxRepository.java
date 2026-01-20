package com.example.pproject.payment.repository;

import com.example.pproject.payment.entity.PgWebhookInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PgWebhookInboxRepository extends JpaRepository<PgWebhookInbox, Long> {

    // PG 이벤트 ID 중복 확인 (웹훅 중복 수신 방지)
    boolean existsByPgEventId(String pgEventId);
}
