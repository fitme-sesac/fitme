package com.example.pproject.faq.repository;

import com.example.pproject.faq.entity.Faq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {

    /**
     * 삭제되지 않은 FAQ 목록 조회 (공개 여부 필터링)
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL AND f.isPublic = :isPublic ORDER BY f.createdAt DESC")
    Page<Faq> findByIsPublicAndNotDeleted(@Param("isPublic") Boolean isPublic, Pageable pageable);

    /**
     * 삭제되지 않은 모든 FAQ 조회
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL ORDER BY f.createdAt DESC")
    Page<Faq> findAllNotDeleted(Pageable pageable);

    /**
     * 키워드로 FAQ 검색 (질문 및 답변)
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL AND (f.question LIKE %:keyword% OR f.answer LIKE %:keyword%) ORDER BY f.createdAt DESC")
    Page<Faq> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 공개 FAQ 중 키워드 검색
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL AND f.isPublic = true AND (f.question LIKE %:keyword% OR f.answer LIKE %:keyword%) ORDER BY f.createdAt DESC")
    Page<Faq> searchPublicByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * ID로 FAQ 조회 (삭제 제외)
     */
    @Query("SELECT f FROM Faq f WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<Faq> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 질문으로 FAQ 존재 여부 확인
     */
    @Query("SELECT COUNT(f) > 0 FROM Faq f WHERE f.question = :question AND f.deletedAt IS NULL")
    boolean existsByQuestionAndNotDeleted(@Param("question") String question);

    /**
     * 공개된 FAQ 목록 (사용자용)
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL AND f.isPublic = true ORDER BY f.createdAt DESC")
    List<Faq> findAllPublicFaqs();
}