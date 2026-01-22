package com.example.pproject.resume.repository;

import com.example.pproject.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // 특정 유저의 이력서 목록 조회
    List<Resume> findAllByUserId(Long userId);

    // 특정 유저의 대표 이력서 조회
    Optional<Resume> findByUserIdAndPrimaryTrue(Long userId);
}