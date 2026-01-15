package com.example.pproject.sms;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "phone_verification")
@Getter @Setter
public class PhoneVerificationEntity {

    @Id
    @Column(name = "verification_id", nullable = false)
    private UUID verificationId;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "purpose", nullable = false, length = 20)
    private String purpose; // SIGNUP / PASSWORD_RESET / PHONE_LINK

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash; // CHAR(64) hex

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "resend_count", nullable = false)
    private int resendCount;

    @Column(name = "last_sent_at", nullable = false)
    private OffsetDateTime lastSentAt;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "request_user_agent")
    private String requestUserAgent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
