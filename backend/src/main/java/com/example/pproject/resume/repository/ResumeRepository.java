package com.example.pproject.resume.repository;

import com.example.pproject.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // 특정 유저의 이력서 목록 조회
    List<Resume> findAllByUserId(Integer userId);

    // 특정 유저의 대표 이력서 조회
    Optional<Resume> findByUserIdAndPrimaryTrue(Integer userId);

    
    // 특정 회원의 대표 이력서 조회 (member_id 기준)
    // UserEntity의 id는 Integer 타입이므로 Long을 Integer로 변환
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Resume r WHERE r.user.id = :memberId AND r.primary = true")
    Optional<Resume> findByMemberIdAndPrimaryTrue(@org.springframework.data.repository.query.Param("memberId") Integer memberId);
