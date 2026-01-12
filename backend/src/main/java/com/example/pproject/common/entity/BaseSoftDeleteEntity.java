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

    /**
     * Marks the entity as deleted by recording the current date and time in the deletion timestamp.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores the entity by clearing its deletion timestamp.
     *
     * After calling this method the entity is no longer considered deleted.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Indicates whether the entity is marked as deleted.
     *
     * @return true if the entity has a deletion timestamp (`deletedAt`), false otherwise.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}