package com.example.pproject.inquiry.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @Column(nullable = false)
    private Long inquiryId;

    @Column(length = 20, nullable = false)
    private String authorType;  // CUSTOMER, ADMIN

    @Column(name = "customer_member_id")
    private Long customerMemberId;

    @Column(name = "admin_member_id")
    private Long adminMemberId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
