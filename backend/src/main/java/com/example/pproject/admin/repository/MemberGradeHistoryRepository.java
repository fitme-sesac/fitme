package com.example.pproject.admin.repository;

import com.example.pproject.admin.entity.MemberGradeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberGradeHistoryRepository extends JpaRepository<MemberGradeHistory, Long> {

    // Entity의 필드명(targetMemberId)과 정확히 일치해야 함!
    Page<MemberGradeHistory> findByTargetMemberId(Long targetMemberId, Pageable pageable);

    Page<MemberGradeHistory> findByAdminMemberId(Long adminMemberId, Pageable pageable);
}