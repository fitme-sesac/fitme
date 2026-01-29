package com.example.pproject.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_grade_history")
@Getter
@Setter // Service에서 setter를 쓰려면 필요
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MemberGradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(name = "target_member_id", nullable = false)
    private Long targetMemberId;

    @Column(name = "admin_member_id", nullable = false)
    private Long adminMemberId;

    @Column(name = "prev_grade")
    private String prevGrade;

    @Column(name = "new_grade")
    private String newGrade;

    @Column(name = "prev_status")
    private String prevStatus;

    @Column(name = "new_status")
    private String newStatus;

    // ✅ [추가] Service에서 사용 중인 필드 추가
    @Column(name = "change_type", length = 20)
    private String changeType; // UPGRADE, WITHDRAW

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    // ✅ [추가] 관리자 메모 필드 추가
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}