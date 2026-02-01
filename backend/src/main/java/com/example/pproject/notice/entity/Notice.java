package com.example.pproject.notice.entity;

import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notice")
@SQLDelete(sql = "UPDATE notice SET deleted_at = now() WHERE notice_id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "notice_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private NoticeType noticeType;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NoticeStatus status;

    @Column(name = "purge_after")
    private LocalDateTime purgeAfter;

    // member 패키지를 임포트하지 않기 위해 Long ID로 관리
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // [추가] 첨부파일 리스트 (1:N 관계)
    // CascadeType.ALL: 공지 삭제 시 첨부파일 데이터도 같이 삭제
    // orphanRemoval = true: 리스트에서 제거하면 DB에서도 삭제
    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Builder 사용 시 null이 아닌 빈 리스트로 초기화
    private List<NoticeAttachment> attachments = new ArrayList<>();

    // ==================== 비즈니스 로직 ====================

    public void update(String title, String body, Boolean isPublic, NoticeType noticeType,
            NoticeStatus status, LocalDateTime purgeAfter, Long updatedBy) {
        this.title = title;
        this.body = body;
        this.isPublic = isPublic;
        this.noticeType = noticeType;
        this.status = status;
        this.purgeAfter = purgeAfter;
        this.updatedBy = updatedBy;
    }

    public void markForDeletion() {
        this.status = NoticeStatus.PENDING_DELETE;
    }

    // [추가] 첨부파일 연관관계 편의 메서드
    public void addAttachment(NoticeAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setNotice(this);
    }

    public static boolean isUniqueActivePolicyType(NoticeType type) {
        return type == NoticeType.TERMS || type == NoticeType.PRIVACY || type == NoticeType.POLICY;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum NoticeType {
        OPS, POLICY, PRIVACY, TERMS, COMMUNITY
    }

    public enum NoticeStatus {
        ACTIVE, PENDING_DELETE, DELETED
    }
}