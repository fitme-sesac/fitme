package com.example.pproject.job.repository;

import com.example.pproject.job.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    
    // 공개 채용공고 검색 (제목, 기술스택, 지역 검색) - 네이티브 쿼리 사용 (stack이 text[] 배열 타입)
    @Query(value = """
        SELECT * FROM job_posting j 
        WHERE j.status = 'OPEN' 
          AND j.deleted_at IS NULL 
          AND (LOWER(j.title) LIKE LOWER('%' || :keyword || '%')
               OR LOWER(array_to_string(j.stack, ',')) LIKE LOWER('%' || :keyword || '%')
               OR LOWER(j.location) LIKE LOWER('%' || :keyword || '%'))
        ORDER BY j.created_at DESC
        """, 
        countQuery = """
        SELECT COUNT(*) FROM job_posting j 
        WHERE j.status = 'OPEN' 
          AND j.deleted_at IS NULL 
          AND (LOWER(j.title) LIKE LOWER('%' || :keyword || '%')
               OR LOWER(array_to_string(j.stack, ',')) LIKE LOWER('%' || :keyword || '%')
               OR LOWER(j.location) LIKE LOWER('%' || :keyword || '%'))
        """,
        nativeQuery = true)
    Page<JobEntity> searchPublicJobs(@Param("keyword") String keyword, Pageable pageable);
    
    // 스택/기술별 공개 채용공고 - 네이티브 쿼리 사용 (stack이 text[] 배열 타입)
    @Query(value = """
        SELECT * FROM job_posting j 
        WHERE j.status = 'OPEN' 
          AND j.deleted_at IS NULL 
          AND EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%'))
        ORDER BY j.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM job_posting j 
        WHERE j.status = 'OPEN' 
          AND j.deleted_at IS NULL 
          AND EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%'))
        """,
        nativeQuery = true)
    Page<JobEntity> findPublicJobsByStack(@Param("stack") String stack, Pageable pageable);
    
    // 지역별 공개 채용공고
    @Query("SELECT j FROM JobEntity j WHERE j.status = 'OPEN' AND j.deletedAt IS NULL AND LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))")
    Page<JobEntity> findPublicJobsByLocation(@Param("location") String location, Pageable pageable);
    
    /**
     * 포지션 카테고리별 공개 채용공고 조회 (stack 기반 도출: 프론트엔드/백엔드/풀스택)
     * - position 값: "프론트엔드", "백엔드", "풀스택" (JobPositionUtil.ORDERED_POSITION_CATEGORIES와 동일)
     */
    @Query(value = """
        WITH pos_derived AS (
          SELECT j.job_id,
            CASE
              WHEN EXISTS (SELECT 1 FROM unnest(j.stack) s WHERE LOWER(s) LIKE '%%react%%' OR LOWER(s) LIKE '%%vue%%' OR LOWER(s) LIKE '%%angular%%' OR LOWER(s) LIKE '%%javascript%%' OR LOWER(s) LIKE '%%typescript%%' OR LOWER(s) LIKE '%%next%%' OR LOWER(s) LIKE '%%frontend%%' OR LOWER(s) LIKE '%%프론트%%')
               AND EXISTS (SELECT 1 FROM unnest(j.stack) s WHERE LOWER(s) LIKE '%%java%%' OR LOWER(s) LIKE '%%spring%%' OR LOWER(s) LIKE '%%python%%' OR LOWER(s) LIKE '%%node%%' OR LOWER(s) LIKE '%%backend%%' OR LOWER(s) LIKE '%%백엔드%%' OR LOWER(s) LIKE '%%sql%%' OR LOWER(s) LIKE '%%mysql%%')
              THEN '풀스택'
              WHEN EXISTS (SELECT 1 FROM unnest(j.stack) s WHERE LOWER(s) LIKE '%%react%%' OR LOWER(s) LIKE '%%vue%%' OR LOWER(s) LIKE '%%angular%%' OR LOWER(s) LIKE '%%javascript%%' OR LOWER(s) LIKE '%%typescript%%' OR LOWER(s) LIKE '%%next%%' OR LOWER(s) LIKE '%%frontend%%' OR LOWER(s) LIKE '%%프론트%%')
              THEN '프론트엔드'
              WHEN EXISTS (SELECT 1 FROM unnest(j.stack) s WHERE LOWER(s) LIKE '%%java%%' OR LOWER(s) LIKE '%%spring%%' OR LOWER(s) LIKE '%%python%%' OR LOWER(s) LIKE '%%node%%' OR LOWER(s) LIKE '%%backend%%' OR LOWER(s) LIKE '%%백엔드%%' OR LOWER(s) LIKE '%%sql%%' OR LOWER(s) LIKE '%%mysql%%')
              THEN '백엔드'
              ELSE NULL
            END AS pos
          FROM job_posting j
          WHERE j.status = 'OPEN' AND j.deleted_at IS NULL
        )
        SELECT j.* FROM job_posting j
        INNER JOIN pos_derived p ON j.job_id = p.job_id
        WHERE p.pos = :position
        ORDER BY j.created_at DESC
        """,
        countQuery = """
        WITH pos_derived AS (
          SELECT j.job_id,
            CASE
              WHEN EXISTS (SELECT 1 FROM unnest(j.stack) s WHERE LOWER(s) LIKE '%%react%%' OR LOWER(s) LIKE '%%vue%%' OR LOWER(s) LIKE '%%angular%%' OR LOWER(s) LIKE '%%javascript%%' OR LOWER(s) LIKE '%%typescript%%' OR LOWER(s) LIKE '%%next%%' OR LOWER(s) LIKE '%%frontend%%' OR LOWER(s) LIKE '%%프론트%%')
               AND EXISTS (SELECT 1 FROM unnest(j.stack) s WHERE LOWER(s) LIKE '%%java%%' OR LOWER(s) LIKE '%%spring%%' OR LOWER(s) LIKE '%%python%%' OR LOWER(s) LIKE '%%node%%' OR LOWER(s) LIKE '%%backend%%' OR LOWER(s) LIKE '%%백엔드%%' OR LOWER(s) LIKE '%%sql%%' OR LOWER(s) LIKE '%%mysql%%')
              THEN '풀스택'
              WHEN EXISTS (SELECT 1 FROM unnest(j.stack) s WHERE LOWER(s) LIKE '%%react%%' OR LOWER(s) LIKE '%%vue%%' OR LOWER(s) LIKE '%%angular%%' OR LOWER(s) LIKE '%%javascript%%' OR LOWER(s) LIKE '%%typescript%%' OR LOWER(s) LIKE '%%next%%' OR LOWER(s) LIKE '%%frontend%%' OR LOWER(s) LIKE '%%프론트%%')
              THEN '프론트엔드'
              WHEN EXISTS (SELECT 1 FROM unnest(j.stack) s WHERE LOWER(s) LIKE '%%java%%' OR LOWER(s) LIKE '%%spring%%' OR LOWER(s) LIKE '%%python%%' OR LOWER(s) LIKE '%%node%%' OR LOWER(s) LIKE '%%backend%%' OR LOWER(s) LIKE '%%백엔드%%' OR LOWER(s) LIKE '%%sql%%' OR LOWER(s) LIKE '%%mysql%%')
              THEN '백엔드'
              ELSE NULL
            END AS pos
          FROM job_posting j
          WHERE j.status = 'OPEN' AND j.deleted_at IS NULL
        )
        SELECT COUNT(*) FROM job_posting j
        INNER JOIN pos_derived p ON j.job_id = p.job_id
        WHERE p.pos = :position
        """,
        nativeQuery = true)
    Page<JobEntity> findPublicJobsByPosition(@Param("position") String position, Pageable pageable);
    
    // ===== 필터 옵션 조회 =====
    
    // 공개 채용공고에서 사용된 모든 스택 목록 조회 (중복 제거)
    @Query(value = """
        SELECT DISTINCT unnest(j.stack) AS stack_item 
        FROM job_posting j 
        WHERE j.status = 'OPEN' 
          AND j.deleted_at IS NULL 
          AND j.stack IS NOT NULL
        ORDER BY stack_item
        """, nativeQuery = true)
    List<String> findDistinctStacks();
    
    // 공개 채용공고에서 사용된 모든 지역 목록 조회 (시/도 단위로 파싱, 중복 제거)
    @Query(value = """
        SELECT DISTINCT 
          CASE 
            WHEN position(' ' in j.location) > 0 THEN split_part(j.location, ' ', 1)
            ELSE j.location
          END AS region
        FROM job_posting j 
        WHERE j.status = 'OPEN' 
          AND j.deleted_at IS NULL 
          AND j.location IS NOT NULL 
          AND j.location != ''
        ORDER BY region
        """, nativeQuery = true)
    List<String> findDistinctLocations();
    
    // 공개 채용공고 단일 조회 (조회수 증가용)
    @Query("SELECT j FROM JobEntity j WHERE j.id = :id AND j.status = 'OPEN' AND j.deletedAt IS NULL")
    Optional<JobEntity> findPublicJobById(@Param("id") Long id);
    
    // 조회수 증가 (embedding 필드 제외하여 pgvector null 바인딩 문제 방지)
    @Modifying
    @Query("UPDATE JobEntity j SET j.viewCount = j.viewCount + 1, j.updatedAt = CURRENT_TIMESTAMP WHERE j.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
