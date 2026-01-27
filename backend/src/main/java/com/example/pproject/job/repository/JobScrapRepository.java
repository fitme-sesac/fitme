package com.example.pproject.job.repository;

import com.example.pproject.job.entity.JobScrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobScrapRepository extends JpaRepository<JobScrap, Long> {

    /**
     * 스크랩 여부 확인
     */
    boolean existsByMemberIdAndJobId(Long memberId, Long jobId);

    /**
     * 스크랩 조회
     */
    Optional<JobScrap> findByMemberIdAndJobId(Long memberId, Long jobId);

    /**
     * 스크랩 삭제
     */
    void deleteByMemberIdAndJobId(Long memberId, Long jobId);

    /**
     * 내 스크랩 목록 조회 (페이징, 최신순)
     */
    Page<JobScrap> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 내 스크랩 목록 조회 (전체, 최신순)
     */
    List<JobScrap> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 스크랩 수 조회
     */
    long countByMemberId(Long memberId);

    /**
     * 특정 공고의 스크랩 수 조회
     */
    long countByJobId(Long jobId);

    /**
     * 삭제되지 않은 공고만 스크랩 목록 조회 (OPEN 상태)
     */
    @Query("SELECT s FROM JobScrap s " +
            "JOIN s.job j " +
            "WHERE s.memberId = :memberId " +
            "AND j.deletedAt IS NULL " +
            "ORDER BY s.createdAt DESC")
    Page<JobScrap> findActiveScrapsByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}
