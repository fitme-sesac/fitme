package com.example.pproject.notice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 공지사항 엔티티
 * ADM-NTC-001, ADM-NTC-003, ADM-POL-001 관련
 */

@Entity
@Table(name = "notice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "notice_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NoticeType noticeType = NoticeType.OPS;

    @Column(name = "is_important", nullable = false)
    @Builder.Default
    private Boolean isImportant = false;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NoticeStatus status = NoticeStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "purge_after")
    private LocalDateTime purgeAfter;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoticeAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoticeDelivery> deliveries = new ArrayList<>();

    /**
     * 소프트 삭제 메서드 (30일 유예 기간 설정)
     */
    public void delete() {
        this.status = NoticeStatus.PENDING_DELETE;
        this.deletedAt = LocalDateTime.now();
        this.purgeAfter = LocalDateTime.now().plusDays(30);
    }

    /**
     * 복구 메서드
     */
    public void restore() {
        this.status = NoticeStatus.ACTIVE;
        this.deletedAt = null;
        this.purgeAfter = null;
    }

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 첨부파일 추가
     */
    public void addAttachment(NoticeAttachment attachment) {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(attachment);
        attachment.setNotice(this);
    }

    /**
     * 발송 기록 추가
     */
    public void addDelivery(NoticeDelivery delivery) {
        if (deliveries == null) {
            deliveries = new ArrayList<>();
        }
        deliveries.add(delivery);
        delivery.setNotice(this);
    }

    /**
     * 공지사항 타입 열거형
     */
    public enum NoticeType {
        POLICY,  // 정책
        OPS,      // 운영
        TERMS,    // 이용 약관 (추가)
        PRIVACY   // 개인정보 처리방침 (추가)
    }

    /**
     * 공지사항 상태 열거형
     */
    public enum NoticeStatus {
        ACTIVE,         // 활성
        PENDING_DELETE, // 삭제 대기중
        DELETED         // 완전 삭제됨
    }
}