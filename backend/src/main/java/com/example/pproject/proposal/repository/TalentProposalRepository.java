package com.example.pproject.proposal.repository;

import com.example.pproject.Constant.ProposalStatus;
import com.example.pproject.proposal.entity.TalentProposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TalentProposalRepository extends JpaRepository<TalentProposal, Long> {

    // 구직자가 받은 제안 목록 (최신순)
    @Query("SELECT p FROM TalentProposal p WHERE p.candidate.id = :candidateId ORDER BY p.createdAt DESC")
    Page<TalentProposal> findByCandidateId(@Param("candidateId") Long candidateId, Pageable pageable);

    // 기업이 보낸 제안 목록
    @Query("SELECT p FROM TalentProposal p WHERE p.employer.id = :employerId ORDER BY p.createdAt DESC")
    Page<TalentProposal> findByEmployerId(@Param("employerId") Long employerId, Pageable pageable);

    // 특정 구직자에게 특정 공고로 이미 제안했는지 확인 (jobId null 가능 - 일반 스카우트 제안)
    @Query("SELECT p FROM TalentProposal p WHERE p.employer.id = :employerId AND p.candidate.id = :candidateId " +
            "AND ((:jobId IS NULL AND p.job IS NULL) OR (p.job IS NOT NULL AND p.job.id = :jobId))")
    Optional<TalentProposal> findByEmployerIdAndCandidateIdAndJobId(
            @Param("employerId") Long employerId, @Param("candidateId") Long candidateId, @Param("jobId") Long jobId);

    // 구직자의 미확인 제안 수
    @Query("SELECT COUNT(p) FROM TalentProposal p WHERE p.candidate.id = :candidateId AND p.status = 'PENDING'")
    long countPendingByCandidateId(@Param("candidateId") Long candidateId);

    // 만료 대상 제안들 조회
    @Query("SELECT p FROM TalentProposal p WHERE p.status IN ('PENDING', 'VIEWED') AND p.expiresAt < :now")
    List<TalentProposal> findExpiredProposals(@Param("now") LocalDateTime now);

    // 상태별 제안 조회 (구직자용)
    @Query("SELECT p FROM TalentProposal p WHERE p.candidate.id = :candidateId AND p.status = :status ORDER BY p.createdAt DESC")
    Page<TalentProposal> findByCandidateIdAndStatus(@Param("candidateId") Long candidateId,
                                                     @Param("status") ProposalStatus status,
                                                     Pageable pageable);
}
