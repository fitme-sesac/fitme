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
     * Retrieve active (non-deleted) FAQs in descending creation order with pagination.
     *
     * @param pageable pagination information (page number, size, sort)
     * @return a page of FAQs that have not been deleted, ordered by createdAt descending
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL ORDER BY f.createdAt DESC")
    Page<Faq> findAllActive(Pageable pageable);

    /**
     * Retrieve publicly visible FAQs that are not deleted, ordered by newest first.
     *
     * @param pageable pagination and sorting information
     * @return a page of public, non-deleted Faq entities ordered by createdAt descending
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL AND f.isPublic = true ORDER BY f.createdAt DESC")
    Page<Faq> findAllPublic(Pageable pageable);

    /**
     * Searches active FAQs whose question or answer contains the given keyword.
     *
     * @param keyword  substring to match against FAQ question or answer (used with SQL LIKE)
     * @param pageable pagination and sorting information for the result page
     * @return         a page of FAQs matching the keyword, ordered by creation date descending
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL " +
            "AND (f.question LIKE %:keyword% OR f.answer LIKE %:keyword%) " +
            "ORDER BY f.createdAt DESC")
    Page<Faq> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Filter FAQs by public visibility.
     *
     * @param isPublic true to select public FAQs, false to select private FAQs
     * @param pageable pagination information for the resulting page
     * @return a page of active FAQs whose `isPublic` equals the provided value, ordered by `createdAt` descending
     */
    @Query("SELECT f FROM Faq f WHERE f.deletedAt IS NULL AND f.isPublic = :isPublic ORDER BY f.createdAt DESC")
    Page<Faq> findByIsPublic(@Param("isPublic") Boolean isPublic, Pageable pageable);

    /**
     * Retrieve a non-deleted FAQ by its ID.
     *
     * @param faqId the ID of the FAQ to retrieve
     * @return an Optional containing the FAQ if found and not deleted, or empty otherwise
     */
    @Query("SELECT f FROM Faq f WHERE f.faqId = :faqId AND f.deletedAt IS NULL")
    Optional<Faq> findByIdActive(@Param("faqId") Long faqId);

    /**
     * Count the FAQs that have not been deleted.
     *
     * @return the total number of FAQs where `deletedAt` is null
     */
    @Query("SELECT COUNT(f) FROM Faq f WHERE f.deletedAt IS NULL")
    Long countActive();
}