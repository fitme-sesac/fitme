package com.example.pproject.resume.repository;

import com.example.pproject.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // 특정 유저의 모든 이력서 조회
    List<Resume> findAllByUserId(Integer userId);

    // 특정 유저의 대표 이력서 단건 조회
    Optional<Resume> findByUserIdAndPrimaryTrue(Integer userId);
}