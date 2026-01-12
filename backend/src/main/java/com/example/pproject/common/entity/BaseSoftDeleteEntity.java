package com.example.pproject.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseSoftDeleteEntity extends BaseTimeEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 삭제 처리 편의 메서드
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    // 복구 처리 편의 메서드
    public void restore() {
        this.deletedAt = null;
    }

    // 삭제 여부 확인
    public boolean isDeleted() {
        return deletedAt != null;
    }
}