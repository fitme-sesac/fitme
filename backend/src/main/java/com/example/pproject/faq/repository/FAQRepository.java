package com.example.pproject.faq.repository;

import com.example.pproject.faq.entity.FAQ;
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
public interface FAQRepository extends JpaRepository<FAQ, Long> {

    // ==================== 기본 조회 (Entity @Where 적용됨) ====================

    Optional<FAQ> findById(Long id);

    @Query("SELECT f FROM FAQ f WHERE f.isPublic = true ORDER BY f.createdAt DESC")
    Page<FAQ> findAllPublic(Pageable pageable);

    @Query("SELECT f FROM FAQ f ORDER BY f.createdAt DESC")
    Page<FAQ> findAllAdmin(Pageable pageable);

    // ==================== 검색 (Entity @Where 적용됨) ====================

    @Query("SELECT f FROM FAQ f WHERE f.isPublic = true AND f.question LIKE %:keyword% ORDER BY f.createdAt DESC")
    Page<FAQ> searchPublic(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT f FROM FAQ f WHERE f.isPublic = true AND (f.question LIKE %:keyword% OR f.answer LIKE %:keyword%) ORDER BY f.createdAt DESC")
    Page<FAQ> searchPublicFull(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT f FROM FAQ f WHERE (f.question LIKE %:keyword% OR f.answer LIKE %:keyword%) ORDER BY f.createdAt DESC")
    Page<FAQ> searchAll(@Param("keyword") String keyword, Pageable pageable);

    // ==================== 공개 여부별 조회 (Entity @Where 적용됨) ====================

    @Query("SELECT f FROM FAQ f WHERE f.isPublic = :isPublic ORDER BY f.createdAt DESC")
    Page<FAQ> findByIsPublic(@Param("isPublic") Boolean isPublic, Pageable pageable);

    @Query("SELECT f FROM FAQ f WHERE f.locked = :locked ORDER BY f.createdAt DESC")
    Page<FAQ> findByLocked(@Param("locked") Boolean locked, Pageable pageable);

    @Query("SELECT f FROM FAQ f WHERE f.isPublic = :isPublic AND f.locked = :locked ORDER BY f.createdAt DESC")
    Page<FAQ> findByIsPublicAndLocked(
            @Param("isPublic") Boolean isPublic,
            @Param("locked") Boolean locked,
            Pageable pageable
    );

    // ==================== 통계용 (Entity @Where 적용됨) ====================

    @Query("SELECT COUNT(f) FROM FAQ f WHERE f.isPublic = true")
    Long countPublic();

    @Query("SELECT COUNT(f) FROM FAQ f")
    Long countAll();

    @Query("SELECT COUNT(f) FROM FAQ f WHERE f.locked = true")
    Long countLocked();

    // ==================== 벌크 작업 (수정됨) ====================

    /**
     * [수정] 이미 삭제된 데이터는 업데이트 대상에서 제외 (AND f.deletedAt IS NULL 추가)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE FAQ f SET f.isPublic = :isPublic WHERE f.id IN :ids AND f.deletedAt IS NULL")
    void bulkUpdatePublic(@Param("ids") List<Long> ids, @Param("isPublic") Boolean isPublic);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE FAQ f SET f.locked = :locked WHERE f.id IN :ids AND f.deletedAt IS NULL")
    void bulkUpdateLocked(@Param("ids") List<Long> ids, @Param("locked") Boolean locked);

    // ==================== 최신 데이터 (수정됨) ====================

    /**
     * [수정] Native Query는 Entity의 @Where가 적용되지 않으므로
     * 반드시 'deleted_at IS NULL' 조건을 직접 명시해야 함
     */
    @Query(value = "SELECT * FROM faq WHERE is_public = true AND deleted_at IS NULL ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<FAQ> findRecentPublic(@Param("limit") int limit);

    @Query(value = "SELECT * FROM faq WHERE is_public = true AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT :limit", nativeQuery = true)
    List<FAQ> findRecentlyUpdatedPublic(@Param("limit") int limit);

    // ==================== 존재 여부 확인 (Entity @Where 적용됨) ====================

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FAQ f WHERE f.isPublic = true")
    boolean existsPublic();

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FAQ f WHERE f.question = :question")
    boolean existsByQuestion(@Param("question") String question);
}