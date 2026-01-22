package com.example.pproject.resume.repository;

import com.example.pproject.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // 특정 유저의 이력서 목록 조회 (Integer 지원 - 하위 호환)
    List<Resume> findAllByUserId(Integer userId);
    
    // 특정 유저의 이력서 목록 조회 (Long 지원)
    List<Resume> findAllByUser_Id(Long userId);

    // 특정 유저의 대표 이력서 조회 (Integer 지원 - 하위 호환)
    Optional<Resume> findByUserIdAndPrimaryTrue(Integer userId);
    
    // 특정 유저의 대표 이력서 조회 (Long 지원)
    Optional<Resume> findByUser_IdAndPrimaryTrue(Long userId);
    
    // 별칭 메서드 (하위 호환성)
    default Optional<Resume> findByMemberIdAndPrimaryTrue(Long memberId) {
        return findByUser_IdAndPrimaryTrue(memberId);
    }

    // 특정 유저의 최근 수정된 이력서 조회 (AI 첨삭 등에서 활용 가능)
    Optional<Resume> findFirstByUserIdOrderByLastModifiedAtDesc(Integer userId);
    
    // 특정 유저의 최근 수정된 이력서 조회 (Long 지원)
    Optional<Resume> findFirstByUser_IdOrderByLastModifiedAtDesc(Long userId);

}