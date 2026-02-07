package com.example.pproject.faq.entity;

import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "faq")
// 1. Repository.delete() 호출 시 실제로는 UPDATE 쿼리가 실행됨 (Soft Delete)
@SQLDelete(sql = "UPDATE faq SET deleted_at = now(), is_public = false WHERE faq_id = ?")
// 2. 조회 시 deleted_at이 NULL인 데이터만 자동으로 가져옴
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

    // ✅ [추가] 삭제 시간 필드 (Soft Delete용)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ==================== 비즈니스 로직 ====================

    public void update(String question, String answer, Boolean locked, Boolean isPublic) {
        this.question = question;
        this.answer = answer;
        this.locked = locked;
        this.isPublic = isPublic;
        this.updatedAt = LocalDateTime.now();
    }

    public void toggleLock() {
        this.locked = !this.locked;
        this.updatedAt = LocalDateTime.now();
    }

    public void lock() {
        this.locked = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void unlock() {
        this.locked = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        this.updatedAt = LocalDateTime.now();
    }

    // 이미 삭제된 상태인지 확인
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.locked == null) this.locked = true;
        if (this.isPublic == null) this.isPublic = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}