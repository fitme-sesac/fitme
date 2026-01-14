package com.example.pproject.faq.repository;

import com.example.pproject.faq.model.Faq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {

    /**
     * 삭제되지 않은 FAQ 목록 조회 (페이지네이션)
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL ORDER BY f.createdAt DESC")
    Page<Faq> findAllActive(Pageable pageable);

    /**
     * 공개된 FAQ만 조회 (회원용)
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL AND f.isPublic = true ORDER BY f.createdAt DESC")
    Page<Faq> findAllPublic(Pageable pageable);

    /**
     * 키워드로 FAQ 검색 (질문 또는 답변)
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL " +
            "AND (f.question LIKE %:keyword% OR f.answer LIKE %:keyword%) " +
            "ORDER BY f.createdAt DESC")
    Page<Faq> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 공개/비공개 상태로 필터링
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL AND f.isPublic = :isPublic ORDER BY f.createdAt DESC")
    Page<Faq> findByIsPublic(@Param("isPublic") Boolean isPublic, Pageable pageable);

    /**
     * ID로 삭제되지 않은 FAQ 조회
     */
    @Query("SELECT f FROM Faq f WHERE f.faqId = :faqId AND f.deletedAt IS NULL")
    Optional<Faq> findByIdActive(@Param("faqId") Long faqId);

    /**
     * 삭제되지 않은 FAQ 전체 개수
     */
    @Query("SELECT COUNT(f) FROM Faq f WHERE f.deletedAt IS NULL")
    Long countActive();
}