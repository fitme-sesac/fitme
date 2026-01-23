package com.example.pproject.faq.entity;

import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "faq")
@SQLDelete(sql = "UPDATE faq SET deleted_at = now() WHERE faq_id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FAQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Long id;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "locked", nullable = false)
    private Boolean locked;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ==================== 비즈니스 로직 ====================

    /**
     * FAQ 업데이트
     */
    public void update(String question, String answer, Boolean locked, Boolean isPublic) {
        this.question = question;
        this.answer = answer;
        this.locked = locked;
        this.isPublic = isPublic;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * FAQ 잠금 상태 토글
     */
    public void toggleLock() {
        this.locked = !this.locked;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * FAQ 잠금
     */
    public void lock() {
        this.locked = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * FAQ 잠금 해제
     */
    public void unlock() {
        this.locked = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * FAQ 공개 여부 설정
     */
    public void setPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * FAQ 공개 상태인지 확인
     */
    public boolean isPublished() {
        return this.isPublic && this.deletedAt == null;
    }

    /**
     * FAQ가 관리자에 의해 잠겨있는지 확인
     */
    public boolean isLockedByAdmin() {
        return this.locked;
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
}