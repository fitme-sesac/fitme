package com.example.pproject.payment.dto.response;

import com.example.pproject.Constant.WebhookProcessStatus;
import com.example.pproject.payment.entity.PgWebhookInbox;

import java.time.LocalDateTime;
import java.util.Map;

public record PgWebhookInboxResponse(
        Long inboxId,
        String eventType,
        WebhookProcessStatus processStatus,
        Integer retryCount,
        String lastError,
        LocalDateTime receivedAt,
        LocalDateTime processedAt,
        Map<String, Object> payload
) {
    public static PgWebhookInboxResponse from(PgWebhookInbox inbox) {
        return new PgWebhookInboxResponse(
                inbox.getInboxId(),
                inbox.getEventType(),
                inbox.getProcessStatus(),
                inbox.getRetryCount(),
                inbox.getLastError(),
                inbox.getReceivedAt(),
                inbox.getProcessedAt(),
                inbox.getPayload()
        );
    }
}
