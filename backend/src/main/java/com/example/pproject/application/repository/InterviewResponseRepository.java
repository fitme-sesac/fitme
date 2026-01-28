package com.example.pproject.application.repository;

import com.example.pproject.application.entity.InterviewResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewResponseRepository extends JpaRepository<InterviewResponseEntity, Long> {

    /**
     * 면접 ID로 응답 이력 조회 (최신순)
     */
    List<InterviewResponseEntity> findByInterviewIdOrderByRespondedAtDesc(Long interviewId);

    /**
     * 면접 ID로 최신 응답 조회
     */
    Optional<InterviewResponseEntity> findFirstByInterviewIdOrderByRespondedAtDesc(Long interviewId);
}
