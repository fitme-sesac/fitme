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

     // 다중 필터 지원 공개 채용공고 검색
     @Query(value = """
               SELECT j.* FROM job_posting j
               LEFT JOIN employer e ON e.employer_id = j.employer_id
               WHERE j.status = 'OPEN'
                 AND j.deleted_at IS NULL
                 AND (:keyword IS NULL OR :keyword = '' OR
                      LOWER(j.title) LIKE LOWER('%' || :keyword || '%')
                      OR LOWER(array_to_string(j.stack, ',')) LIKE LOWER('%' || :keyword || '%'))
                 AND (:stack IS NULL OR :stack = '' OR
                      EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%')))
                 AND (:location IS NULL OR :location = '' OR
                      LOWER(j.location) LIKE LOWER('%' || :location || '%'))
                 AND (:minExperience IS NULL OR j.required_experience >= :minExperience)
                 AND (:maxExperience IS NULL OR j.required_experience <= :maxExperience)
                 AND (:positionKeywords IS NULL OR :positionKeywords = '' OR
                      LOWER(array_to_string(j.stack, ',')) LIKE ANY(string_to_array(LOWER(:positionKeywords), ',')))
                 AND (:industryKeywords IS NULL OR :industryKeywords = '' OR
                      LOWER(COALESCE(e.industry, '')) LIKE ANY(string_to_array(LOWER(:industryKeywords), ',')))
               ORDER BY j.created_at DESC
               """, countQuery = """
               SELECT COUNT(*) FROM job_posting j
               LEFT JOIN employer e ON e.employer_id = j.employer_id
               WHERE j.status = 'OPEN'
                 AND j.deleted_at IS NULL
                 AND (:keyword IS NULL OR :keyword = '' OR
                      LOWER(j.title) LIKE LOWER('%' || :keyword || '%')
                      OR LOWER(array_to_string(j.stack, ',')) LIKE LOWER('%' || :keyword || '%'))
                 AND (:stack IS NULL OR :stack = '' OR
                      EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%')))
                 AND (:location IS NULL OR :location = '' OR
                      LOWER(j.location) LIKE LOWER('%' || :location || '%'))
                 AND (:minExperience IS NULL OR j.required_experience >= :minExperience)
                 AND (:maxExperience IS NULL OR j.required_experience <= :maxExperience)
                 AND (:positionKeywords IS NULL OR :positionKeywords = '' OR
                      LOWER(array_to_string(j.stack, ',')) LIKE ANY(string_to_array(LOWER(:positionKeywords), ',')))
                 AND (:industryKeywords IS NULL OR :industryKeywords = '' OR
                      LOWER(COALESCE(e.industry, '')) LIKE ANY(string_to_array(LOWER(:industryKeywords), ',')))
               """, nativeQuery = true)
     Page<JobEntity> findPublicJobsWithFilters(
               @Param("keyword") String keyword,
               @Param("stack") String stack,
               @Param("location") String location,
               @Param("minExperience") Integer minExperience,
               @Param("maxExperience") Integer maxExperience,
               @Param("positionKeywords") String positionKeywords,
               @Param("industryKeywords") String industryKeywords,
               Pageable pageable);

     // 다중 필터 지원 공개 채용공고 카운트
     @Query(value = """
               SELECT COUNT(*) FROM job_posting j
               LEFT JOIN employer e ON e.employer_id = j.employer_id
               WHERE j.status = 'OPEN'
                 AND j.deleted_at IS NULL
                 AND (:keyword IS NULL OR :keyword = '' OR
                      LOWER(j.title) LIKE LOWER('%' || :keyword || '%')
                      OR LOWER(array_to_string(j.stack, ',')) LIKE LOWER('%' || :keyword || '%'))
                 AND (:stack IS NULL OR :stack = '' OR
                      EXISTS (SELECT 1 FROM unnest(j.stack) AS s WHERE LOWER(s) LIKE LOWER('%' || :stack || '%')))
                 AND (:location IS NULL OR :location = '' OR
                      LOWER(j.location) LIKE LOWER('%' || :location || '%'))
                 AND (:minExperience IS NULL OR j.required_experience >= :minExperience)
                 AND (:maxExperience IS NULL OR j.required_experience <= :maxExperience)
                 AND (:positionKeywords IS NULL OR :positionKeywords = '' OR
                      LOWER(array_to_string(j.stack, ',')) LIKE ANY(string_to_array(LOWER(:positionKeywords), ',')))
                 AND (:industryKeywords IS NULL OR :industryKeywords = '' OR
                      LOWER(COALESCE(e.industry, '')) LIKE ANY(string_to_array(LOWER(:industryKeywords), ',')))
               """, nativeQuery = true)
     long countPublicJobsWithFilters(
               @Param("keyword") String keyword,
               @Param("stack") String stack,
               @Param("location") String location,
               @Param("minExperience") Integer minExperience,
               @Param("maxExperience") Integer maxExperience,
               @Param("positionKeywords") String positionKeywords,
               @Param("industryKeywords") String industryKeywords);

     // 공개 채용공고 검색 (Admin용 등)
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

     // ===== [V3: 최적화된 매칭 엔진 (Recall CTE 패턴)] =====

     /**
      * [홈 화면 전용: AI 추천 고속 검색]
      * - 성능 최적화(Recall CTE): HNSW 인덱스를 활용해 상위 후보군(200개)을 먼저 초고속으로 추출
      */
     @Query(value = """
               WITH user_vec AS (
                   SELECT CAST(:userEmbedding AS vector) as u_vec
               ),
               recall AS (
                   -- 1단계: HNSW Index Scan으로 상위 200개 후보 추출
                   SELECT
                       j.job_id,
                       1 - (j.embedding <=> uv.u_vec) AS similarity
                   FROM job_posting j
                   CROSS JOIN user_vec uv
                   WHERE j.status = 'OPEN'
                     AND j.deleted_at IS NULL
                     AND j.embedding IS NOT NULL
                   ORDER BY j.embedding <=> uv.u_vec
                   LIMIT 200
               )
               -- 2단계: 추출된 후보의 상세 정보 조회 (추천 순 정렬)
               SELECT
                   j.job_id,
                   j.employer_id,
                   j.title,
                   j.location,
                   array_to_string(j.stack, ','),
                   j.salary_text,
                   j.summary,
                   j.status,
                   r.similarity,
                   e.name AS company_name,
                   e.logo_url AS company_logo_url,
                   j.created_at,
                   j.required_experience
               FROM recall r
               JOIN job_posting j ON j.job_id = r.job_id
               LEFT JOIN employer e ON e.employer_id = j.employer_id
               WHERE r.similarity >= :minSimilarity
                 AND (:location IS NULL OR j.location LIKE '%' || :location || '%')
                 AND (:skills IS NULL OR array_to_string(j.stack, ',') LIKE '%' || :skills || '%')
               ORDER BY r.similarity DESC
               LIMIT :limit
               """, nativeQuery = true)
     List<Object[]> findJobsBySimilarity(
               @Param("userEmbedding") String userEmbedding,
               @Param("minSimilarity") double minSimilarity,
               @Param("location") String location,
               @Param("skills") String skills,
               @Param("limit") int limit);

     /**
      * [채용공고 페이지: 검색/필터 + 매칭률 정렬 통합 쿼리]
      * - 다중 필터(키워드, 지역, 경력 등)를 적용하면서 매칭률 순으로 정렬
      * - 성능 최적화: Recall CTE 패턴으로 상위 후보군을 먼저 확보한 후 필터링 수행
      */
     @Query(value = """
               WITH user_vec AS (
                   SELECT CAST(:userEmbedding AS vector) as u_vec
               ),
               recall AS (
                   -- [고성능 검색 포인트 1] 전체 공고 중 나에게 맞는 후보군 1000개 추출
                   -- ORDER BY (embedding <=> uv.u_vec)는 HNSW 인덱스를 순차적으로 태울 수 있어
                   -- 대량 데이터에서도 매우 빠르게 유사한 것부터 뽑아낼 수 있습니다. (Recall 기법)
                   SELECT
                       j.job_id,
                       1 - (j.embedding <=> uv.u_vec) AS similarity
                   FROM job_posting j
                   CROSS JOIN user_vec uv
                   WHERE j.status = 'OPEN'
                     AND j.deleted_at IS NULL
                     AND j.embedding IS NOT NULL
                   ORDER BY j.embedding <=> uv.u_vec
                   LIMIT 1000
               )
               -- 후보군 내에서 실시간 검색/필터 적용
               SELECT j.*
               FROM recall r
               JOIN job_posting j ON j.job_id = r.job_id
               LEFT JOIN employer e ON e.employer_id = j.employer_id
               WHERE r.similarity >= :minSimilarity
                 AND (:keyword IS NULL OR j.title LIKE '%' || :keyword || '%' OR j.summary LIKE '%' || :keyword || '%')
                 AND (:stack IS NULL OR array_to_string(j.stack, ',') LIKE '%' || :stack || '%')
                 AND (:location IS NULL OR j.location LIKE '%' || :location || '%')
                 AND (:minExperience IS NULL OR j.required_experience >= :minExperience)
                 AND (:maxExperience IS NULL OR j.required_experience <= :maxExperience)
                 AND (:positionKeywords IS NULL OR :positionKeywords = '' OR
                      LOWER(array_to_string(j.stack, ',')) LIKE ANY(string_to_array(LOWER(:positionKeywords), ',')))
                 AND (:industryKeywords IS NULL OR :industryKeywords = '' OR
                      LOWER(COALESCE(e.industry, '')) LIKE ANY(string_to_array(LOWER(:industryKeywords), ',')))
               ORDER BY r.similarity DESC
               """, countQuery = """
               WITH user_vec AS (
                   SELECT CAST(:userEmbedding AS vector) as u_vec
               ),
               recall AS (
                   SELECT
                       j.job_id,
                       1 - (j.embedding <=> uv.u_vec) AS similarity
                   FROM job_posting j
                   CROSS JOIN user_vec uv
                   WHERE j.status = 'OPEN'
                     AND j.deleted_at IS NULL
                     AND j.embedding IS NOT NULL
                   ORDER BY j.embedding <=> uv.u_vec
                   LIMIT 1000
               )
               SELECT COUNT(j.job_id)
               FROM recall r
               JOIN job_posting j ON j.job_id = r.job_id
               LEFT JOIN employer e ON e.employer_id = j.employer_id
               WHERE r.similarity >= :minSimilarity
                 AND (:keyword IS NULL OR j.title LIKE '%' || :keyword || '%' OR j.summary LIKE '%' || :keyword || '%')
                 AND (:stack IS NULL OR array_to_string(j.stack, ',') LIKE '%' || :stack || '%')
                 AND (:location IS NULL OR j.location LIKE '%' || :location || '%')
                 AND (:minExperience IS NULL OR j.required_experience >= :minExperience)
                 AND (:maxExperience IS NULL OR j.required_experience <= :maxExperience)
                 AND (:positionKeywords IS NULL OR :positionKeywords = '' OR
                      LOWER(array_to_string(j.stack, ',')) LIKE ANY(string_to_array(LOWER(:positionKeywords), ',')))
                 AND (:industryKeywords IS NULL OR :industryKeywords = '' OR
                      LOWER(COALESCE(e.industry, '')) LIKE ANY(string_to_array(LOWER(:industryKeywords), ',')))
               """, nativeQuery = true)
     Page<JobEntity> findPublicJobsWithSimilarity(
               @Param("userEmbedding") String userEmbedding,
               @Param("minSimilarity") double minSimilarity,
               @Param("keyword") String keyword,
               @Param("stack") String stack,
               @Param("location") String location,
               @Param("minExperience") Integer minExperience,
               @Param("maxExperience") Integer maxExperience,
               @Param("positionKeywords") String positionKeywords,
               @Param("industryKeywords") String industryKeywords,
               Pageable pageable);

     // ===== 필터 옵션 조회 =====

     @Query(value = "SELECT DISTINCT unnest(j.stack) FROM job_posting j WHERE j.status = 'OPEN' AND j.deleted_at IS NULL", nativeQuery = true)
     List<String> findDistinctStacks();

     @Query(value = """
               SELECT DISTINCT
                 CASE WHEN position(' ' in j.location) > 0 THEN split_part(j.location, ' ', 1) ELSE j.location END
               FROM job_posting j WHERE j.status = 'OPEN' AND j.deleted_at IS NULL
               """, nativeQuery = true)
     List<String> findDistinctLocations();

     @Query(value = "SELECT DISTINCT e.industry FROM job_posting j JOIN employer e ON e.employer_id = j.employer_id WHERE j.status = 'OPEN'", nativeQuery = true)
     List<String> findDistinctIndustries();

     @Modifying
     @Query("UPDATE JobEntity j SET j.viewCount = j.viewCount + 1, j.updatedAt = CURRENT_TIMESTAMP WHERE j.id = :id")
     void incrementViewCount(@Param("id") Long id);

     @Query("SELECT j FROM JobEntity j WHERE j.id = :id AND j.status = 'OPEN' AND j.deletedAt IS NULL")
     Optional<JobEntity> findPublicJobById(@Param("id") Long id);

     @Query(value = "SELECT array_to_string(j.stack, ',') FROM job_posting j WHERE j.status = 'OPEN' AND j.deleted_at IS NULL AND j.stack IS NOT NULL", nativeQuery = true)
     List<String> findAllStacksAsStrings();
}
