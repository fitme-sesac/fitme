package com.example.pproject.application.repository;

import com.example.pproject.application.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    // 특정 유저의 지원 내역 조회 (최신순)
    List<JobApplication> findByMemberIdOrderByAppliedAtDesc(Long memberId);

    // 중복 지원 확인
    boolean existsByJobIdAndMemberId(Long jobId, Long memberId);

    // 이력서 삭제 시 참조 확인
    boolean existsByResumeId(Long resumeId);
}