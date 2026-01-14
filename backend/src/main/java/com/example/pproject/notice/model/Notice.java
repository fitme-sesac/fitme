package com.example.pproject.notice.model;

import com.example.pproject.Constant.NoticeType;
import com.example.pproject.Constant.NoticeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 공지사항 엔티티
 * - 관리자(SERVICEADMIN)가 등록/수정/삭제
 * - 회원이 조회 (공개된 공지사항만)
 * - 공지사항과 정책문서(약관, 개인정보, 정책) 통합 관리
 */
@Entity
@Table(name = "notice")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_type", nullable = false, length = 30)
    @Builder.Default
    private NoticeType noticeType = NoticeType.OPS;  // OPS(공지), POLICY(정책), PRIVACY(개인정보), TERMS(약관)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NoticeStatus status = NoticeStatus.ACTIVE;  // ACTIVE(활성), PENDING_DELETE(삭제예정), DELETED(삭제됨)

    @Column(name = "purge_after")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime purgeAfter;  // 언제 완전 삭제될지 (30일 후)

    @Column(name = "created_by")
    private Long createdBy;  // 작성 관리자 ID

    @Column(name = "updated_by")
    private Long updatedBy;  // 수정 관리자 ID

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;

    /**
     * 논리 삭제 여부 확인
     */
    @Transient
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 논리 삭제 처리 (30일 후 완전 삭제)
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = NoticeStatus.PENDING_DELETE;
        // 현재로부터 30일 후 삭제
        this.purgeAfter = LocalDateTime.now().plusDays(30);
    }
}