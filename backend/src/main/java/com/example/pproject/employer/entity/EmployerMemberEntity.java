package com.example.pproject.employer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 기업-회원 소속 매핑 테이블 (ERD: employer_member)
 * - 한 회원이 여러 기업에 소속될 수 있음
 * - 한 기업에 여러 회원이 소속될 수 있음
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "employer_member", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employer_id", "member_id"})
})
@Entity
public class EmployerMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employer_member_id")
    private Long id;

    // 기업 FK
    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    // 회원 FK (ERD: BIGINT)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 회사 내 역할: OWNER, HR, STAFF
    @Column(name = "role_in_company", nullable = false, length = 20)
    private String roleInCompany;

    // 활성 상태
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (active == null) {
            active = true;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
