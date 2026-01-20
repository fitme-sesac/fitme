package com.example.pproject.job.repository;

import com.example.pproject.job.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, Long> {
    
    Optional<JobEntity> findByJobUid(UUID jobUid);
    
    // 기업별 채용공고 목록 (삭제되지 않은 것만)
    @Query("SELECT j FROM JobEntity j WHERE j.employerId = :employerId AND j.deletedAt IS NULL ORDER BY j.createdAt DESC")
    List<JobEntity> findByEmployerIdAndNotDeleted(@Param("employerId") Long employerId);
    
    // 기업별 채용공고 페이징
    @Query("SELECT j FROM JobEntity j WHERE j.employerId = :employerId AND j.deletedAt IS NULL")
    Page<JobEntity> findByEmployerIdAndNotDeleted(@Param("employerId") Long employerId, Pageable pageable);
    
    // 활성화된 채용공고 개수
    @Query("SELECT COUNT(j) FROM JobEntity j WHERE j.employerId = :employerId AND j.status = 'ACTIVE' AND j.deletedAt IS NULL")
    long countActiveByEmployerId(@Param("employerId") Long employerId);
    
    // 전체 지원자 수 합계
    @Query("SELECT COALESCE(SUM(j.applicationCount), 0) FROM JobEntity j WHERE j.employerId = :employerId AND j.deletedAt IS NULL")
    long sumApplicationCountByEmployerId(@Param("employerId") Long employerId);
}
