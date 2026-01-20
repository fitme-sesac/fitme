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

@Repository
public interface JobRepository extends JpaRepository<JobEntity, Long> {
    
    // 기업별 채용공고 목록 (삭제되지 않은 것만)
    @Query("SELECT j FROM JobEntity j WHERE j.employerId = :employerId AND j.deletedAt IS NULL ORDER BY j.createdAt DESC")
    List<JobEntity> findByEmployerIdAndNotDeleted(@Param("employerId") Long employerId);
    
    // 기업별 채용공고 페이징
    @Query("SELECT j FROM JobEntity j WHERE j.employerId = :employerId AND j.deletedAt IS NULL")
    Page<JobEntity> findByEmployerIdAndNotDeleted(@Param("employerId") Long employerId, Pageable pageable);
    
    // 활성화된 채용공고 개수 (OPEN)
    @Query("SELECT COUNT(j) FROM JobEntity j WHERE j.employerId = :employerId AND j.status = 'OPEN' AND j.deletedAt IS NULL")
    long countActiveByEmployerId(@Param("employerId") Long employerId);
    
    // 전체 지원자 수 합계
    @Query("SELECT COALESCE(SUM(j.applicationCount), 0) FROM JobEntity j WHERE j.employerId = :employerId AND j.deletedAt IS NULL")
    long sumApplicationCountByEmployerId(@Param("employerId") Long employerId);
    
    // ID로 삭제되지 않은 채용공고 조회
    @Query("SELECT j FROM JobEntity j WHERE j.id = :id AND j.deletedAt IS NULL")
    Optional<JobEntity> findByIdAndNotDeleted(@Param("id") Long id);
    
    // ===== 공개 채용공고 조회 (status='OPEN', 삭제되지 않은 것) =====
    
    // 공개 채용공고 목록 페이징
    @Query("SELECT j FROM JobEntity j WHERE j.status = 'OPEN' AND j.deletedAt IS NULL")
    Page<JobEntity> findPublicJobs(Pageable pageable);
    
    // 공개 채용공고 검색 (제목, 회사명 검색)
    @Query("""
        SELECT j FROM JobEntity j 
        WHERE j.status = 'OPEN' 
          AND j.deletedAt IS NULL 
          AND (LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(j.stack) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<JobEntity> searchPublicJobs(@Param("keyword") String keyword, Pageable pageable);
    
    // 스택/기술별 공개 채용공고
    @Query("SELECT j FROM JobEntity j WHERE j.status = 'OPEN' AND j.deletedAt IS NULL AND LOWER(j.stack) LIKE LOWER(CONCAT('%', :stack, '%'))")
    Page<JobEntity> findPublicJobsByStack(@Param("stack") String stack, Pageable pageable);
    
    // 지역별 공개 채용공고
    @Query("SELECT j FROM JobEntity j WHERE j.status = 'OPEN' AND j.deletedAt IS NULL AND LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))")
    Page<JobEntity> findPublicJobsByLocation(@Param("location") String location, Pageable pageable);
    
    // 공개 채용공고 단일 조회 (조회수 증가용)
    @Query("SELECT j FROM JobEntity j WHERE j.id = :id AND j.status = 'OPEN' AND j.deletedAt IS NULL")
    Optional<JobEntity> findPublicJobById(@Param("id") Long id);
}
