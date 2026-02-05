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
public interface JobEntityRepository extends JpaRepository<JobEntity, Long> {

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

  // 전체 조회수 합계 (기업 대시보드용)
  @Query("SELECT COALESCE(SUM(j.viewCount), 0) FROM JobEntity j WHERE j.employerId = :employerId AND j.deletedAt IS NULL")
  long sumViewCountByEmployerId(@Param("employerId") Long employerId);

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
      """, countQuery = """
      SELECT COUNT(*) FROM job_posting j
      WHERE j.status = 'OPEN'
        AND j.deleted_at IS NULL
        AND (LOWER(j.title) LIKE LOWER('%' || :keyword || '%')
             OR LOWER(array_to_string(j.stack, ',')) LIKE LOWER('%' || :keyword || '%')
             OR LOWER(j.location) LIKE LOWER('%' || :keyword || '%'))
      """, nativeQuery = true)
  Page<JobEntity> searchPublicJobs(@Param("keyword") String keyword, Pageable pageable);

  // 스택/기술별 공개 채용공고 - 네이티브 쿼리 사용 (stack이 text[] 배열 타입)
  @Query(value = """
      SELECT * FROM job_posting j
      WHERE j.status = 'OPEN'
        AND j.deleted_at IS NULL
        AND EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%'))
      ORDER BY j.created_at DESC
      """, countQuery = """
      SELECT COUNT(*) FROM job_posting j
      WHERE j.status = 'OPEN'
        AND j.deleted_at IS NULL
        AND EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%'))
      """, nativeQuery = true)
  Page<JobEntity> findPublicJobsByStack(@Param("stack") String stack, Pageable pageable);

  // 지역별 공개 채용공고
  @Query("SELECT j FROM JobEntity j WHERE j.status = 'OPEN' AND j.deletedAt IS NULL AND LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))")
  Page<JobEntity> findPublicJobsByLocation(@Param("location") String location, Pageable pageable);

  // 다중 필터 지원 공개 채용공고 검색 - 네이티브 쿼리 사용 (포지션 키워드 + industry 필터 포함)
  @Query(value = """
      SELECT j.* FROM job_posting j
      LEFT JOIN employer e ON e.employer_id = j.employer_id
      LEFT JOIN LATERAL (
          SELECT
              (SELECT MIN((m[1])::int) FROM regexp_matches(replace(coalesce(j.salary_text, ''), ',', ''), '([0-9]{3,5})', 'g') AS m) AS sal_min,
              (SELECT MAX((m[1])::int) FROM regexp_matches(replace(coalesce(j.salary_text, ''), ',', ''), '([0-9]{3,5})', 'g') AS m) AS sal_max
      ) sal ON TRUE
      WHERE j.status = 'OPEN'
        AND j.deleted_at IS NULL
        AND (:keyword IS NULL OR :keyword = '' OR
             LOWER(j.title) LIKE LOWER('%' || :keyword || '%')
             OR LOWER(array_to_string(j.stack, ',')) LIKE LOWER('%' || :keyword || '%')
             OR LOWER(j.location) LIKE LOWER('%' || :keyword || '%'))
        AND (:stack IS NULL OR :stack = '' OR
             EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%')))
        AND (:location IS NULL OR :location = '' OR
             LOWER(j.location) LIKE LOWER('%' || :location || '%'))
        AND (:minExperience IS NULL OR j.required_experience >= :minExperience)
        AND (:maxExperience IS NULL OR j.required_experience <= :maxExperience)
        AND (:salaryMin IS NULL OR (sal.sal_max IS NOT NULL AND sal.sal_max >= :salaryMin))
        AND (:salaryMax IS NULL OR (sal.sal_min IS NOT NULL AND sal.sal_min <= :salaryMax))
        AND (:positionKeywords IS NULL OR :positionKeywords = '' OR
             EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE ANY(string_to_array(:positionKeywords, ','))))
        AND (:industryKeywords IS NULL OR :industryKeywords = '' OR
             LOWER(COALESCE(e.industry, '')) LIKE ANY(string_to_array(:industryKeywords, ',')))
      ORDER BY j.created_at DESC
      """, countQuery = """
      SELECT COUNT(*) FROM job_posting j
      LEFT JOIN employer e ON e.employer_id = j.employer_id
      LEFT JOIN LATERAL (
          SELECT
              (SELECT MIN((m[1])::int) FROM regexp_matches(replace(coalesce(j.salary_text, ''), ',', ''), '([0-9]{3,5})', 'g') AS m) AS sal_min,
              (SELECT MAX((m[1])::int) FROM regexp_matches(replace(coalesce(j.salary_text, ''), ',', ''), '([0-9]{3,5})', 'g') AS m) AS sal_max
      ) sal ON TRUE
      WHERE j.status = 'OPEN'
        AND j.deleted_at IS NULL
        AND (:keyword IS NULL OR :keyword = '' OR
             LOWER(j.title) LIKE LOWER('%' || :keyword || '%')
             OR LOWER(array_to_string(j.stack, ',')) LIKE LOWER('%' || :keyword || '%')
             OR LOWER(j.location) LIKE LOWER('%' || :keyword || '%'))
        AND (:stack IS NULL OR :stack = '' OR
             EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%')))
        AND (:location IS NULL OR :location = '' OR
             LOWER(j.location) LIKE LOWER('%' || :location || '%'))
        AND (:minExperience IS NULL OR j.required_experience >= :minExperience)
        AND (:maxExperience IS NULL OR j.required_experience <= :maxExperience)
        AND (:salaryMin IS NULL OR (sal.sal_max IS NOT NULL AND sal.sal_max >= :salaryMin))
        AND (:salaryMax IS NULL OR (sal.sal_min IS NOT NULL AND sal.sal_min <= :salaryMax))
        AND (:positionKeywords IS NULL OR :positionKeywords = '' OR
             EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE ANY(string_to_array(:positionKeywords, ','))))
        AND (:industryKeywords IS NULL OR :industryKeywords = '' OR
             LOWER(COALESCE(e.industry, '')) LIKE ANY(string_to_array(:industryKeywords, ',')))
      """, nativeQuery = true)
  Page<JobEntity> findPublicJobsWithFilters(
      @Param("keyword") String keyword,
      @Param("stack") String stack,
      @Param("location") String location,
      @Param("minExperience") Integer minExperience,
      @Param("maxExperience") Integer maxExperience,
      @Param("positionKeywords") String positionKeywords,
      @Param("industryKeywords") String industryKeywords,
      @Param("salaryMin") Integer salaryMin,
      @Param("salaryMax") Integer salaryMax,
      Pageable pageable);

  // 다중 필터 지원 공개 채용공고 카운트 - 포지션별 카운트용
  @Query(value = """
      SELECT COUNT(*) FROM job_posting j
      LEFT JOIN employer e ON e.employer_id = j.employer_id
      LEFT JOIN LATERAL (
          SELECT
              (SELECT MIN((m[1])::int) FROM regexp_matches(replace(coalesce(j.salary_text, ''), ',', ''), '([0-9]{3,5})', 'g') AS m) AS sal_min,
              (SELECT MAX((m[1])::int) FROM regexp_matches(replace(coalesce(j.salary_text, ''), ',', ''), '([0-9]{3,5})', 'g') AS m) AS sal_max
      ) sal ON TRUE
      WHERE j.status = 'OPEN'
        AND j.deleted_at IS NULL
        AND (:keyword IS NULL OR :keyword = '' OR
             LOWER(j.title) LIKE LOWER('%' || :keyword || '%')
             OR LOWER(array_to_string(j.stack, ',')) LIKE LOWER('%' || :keyword || '%')
             OR LOWER(j.location) LIKE LOWER('%' || :keyword || '%'))
        AND (:stack IS NULL OR :stack = '' OR
             EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%')))
        AND (:location IS NULL OR :location = '' OR
             LOWER(j.location) LIKE LOWER('%' || :location || '%'))
        AND (:minExperience IS NULL OR j.required_experience >= :minExperience)
        AND (:maxExperience IS NULL OR j.required_experience <= :maxExperience)
        AND (:salaryMin IS NULL OR (sal.sal_max IS NOT NULL AND sal.sal_max >= :salaryMin))
        AND (:salaryMax IS NULL OR (sal.sal_min IS NOT NULL AND sal.sal_min <= :salaryMax))
        AND (:positionKeywords IS NULL OR :positionKeywords = '' OR
             EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE ANY(string_to_array(:positionKeywords, ','))))
        AND (:industryKeywords IS NULL OR :industryKeywords = '' OR
             LOWER(COALESCE(e.industry, '')) LIKE ANY(string_to_array(:industryKeywords, ',')))
      """, nativeQuery = true)
  long countPublicJobsWithFilters(
      @Param("keyword") String keyword,
      @Param("stack") String stack,
      @Param("location") String location,
      @Param("minExperience") Integer minExperience,
      @Param("maxExperience") Integer maxExperience,
      @Param("positionKeywords") String positionKeywords,
      @Param("industryKeywords") String industryKeywords,
      @Param("salaryMin") Integer salaryMin,
      @Param("salaryMax") Integer salaryMax);

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

  // 공개 채용공고의 모든 스택 배열 조회 (포지션 도출용)
  @Query(value = """
      SELECT array_to_string(j.stack, ',') AS stack_str
      FROM job_posting j
      WHERE j.status = 'OPEN'
        AND j.deleted_at IS NULL
        AND j.stack IS NOT NULL
        AND array_length(j.stack, 1) > 0
      """, nativeQuery = true)
  List<String> findAllStacksAsStrings();

  // 공개 채용공고의 기업 업종 목록 조회 (서비스 분야 필터용)
  @Query(value = """
      SELECT DISTINCT e.industry
      FROM job_posting j
      JOIN employer e ON e.employer_id = j.employer_id
      WHERE j.status = 'OPEN'
        AND j.deleted_at IS NULL
        AND e.deleted_at IS NULL
        AND e.industry IS NOT NULL
        AND e.industry != ''
      ORDER BY e.industry
      """, nativeQuery = true)
  List<String> findDistinctIndustries();

  // 공개 채용공고 단일 조회 (조회수 증가용)
  @Query("SELECT j FROM JobEntity j WHERE j.id = :id AND j.status = 'OPEN' AND j.deletedAt IS NULL")
  Optional<JobEntity> findPublicJobById(@Param("id") Long id);

  // 조회수 증가 (embedding 필드 제외하여 pgvector null 바인딩 문제 방지)
  @Modifying
  @Query("UPDATE JobEntity j SET j.viewCount = j.viewCount + 1, j.updatedAt = CURRENT_TIMESTAMP WHERE j.id = :id")
  void incrementViewCount(@Param("id") Long id);

  // ===== 이력서 기반 채용공고 추천 (순수 유사도) =====

  /**
   * [이력서 기반 채용공고 추천 - Native Query]
   * - 사용자 이력서 embedding과 채용공고 embedding 간의 유사도 계산
   * - 순수 유사도 순으로 정렬 (광고/입찰가 무관)
   * - 선택적 지역/스택 필터링
   */
  @Query(value = """
      SELECT
          j.job_id,
          j.employer_id,
          j.title,
          j.location,
          array_to_string(j.stack, ','),
          j.salary_text,
          j.summary,
          j.status,
          1 - (j.embedding <=> CAST(:userEmbedding AS vector)) AS similarity
      FROM job_posting j
      WHERE j.status = 'OPEN'
        AND j.deleted_at IS NULL
        AND j.embedding IS NOT NULL
        AND (1 - (j.embedding <=> CAST(:userEmbedding AS vector))) >= :minSimilarity
        AND (:location IS NULL OR j.location LIKE '%' || :location || '%')
        AND (:skills IS NULL OR array_to_string(j.stack, ',') LIKE '%' || :skills || '%')
      ORDER BY similarity DESC
      LIMIT :limit
      """, nativeQuery = true)
  List<Object[]> findJobsBySimilarity(
      @Param("userEmbedding") String userEmbedding,
      @Param("minSimilarity") double minSimilarity,
      @Param("location") String location,
      @Param("skills") String skills,
      @Param("limit") int limit);
}
