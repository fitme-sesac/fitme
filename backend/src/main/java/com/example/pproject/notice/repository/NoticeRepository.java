package com.example.pproject.notice.repository;

import com.example.pproject.notice.entity.Notice;
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
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 활성 공지사항 목록 조회 (삭제 제외)
     */
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL AND n.status = 'ACTIVE' ORDER BY n.createdAt DESC")
    Page<Notice> findAllActive(Pageable pageable);

    /**
     * 공지사항 타입별 조회
     */
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL AND n.status = 'ACTIVE' AND n.noticeType = :type ORDER BY n.createdAt DESC")
    Page<Notice> findByNoticeType(@Param("type") Notice.NoticeType type, Pageable pageable);

    /**
     * 중요 공지사항 조회
     */
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL AND n.status = 'ACTIVE' AND n.isImportant = true ORDER BY n.createdAt DESC")
    Page<Notice> findImportantNotices(Pageable pageable);

    /**
     * 제목으로 검색
     */
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL AND n.status = 'ACTIVE' AND n.title LIKE %:keyword% ORDER BY n.createdAt DESC")
    Page<Notice> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * ID로 공지사항 조회 (삭제 제외)
     */
    @Query("SELECT n FROM Notice n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<Notice> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 공개 공지사항 목록 (사용자용)
     */
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL AND n.status = 'ACTIVE' AND n.isPublic = true ORDER BY n.isImportant DESC, n.createdAt DESC")
    Page<Notice> findAllPublic(Pageable pageable);

    /**
     * 정책 동의서 목록 조회
     */
    // 1. [추가] 중복 정책 방지를 위한 존재 여부 확인
    boolean existsByNoticeTypeAndStatus(Notice.NoticeType noticeType, Notice.NoticeStatus status);
    // 2. [수정] 정책 동의서 목록 조회 (모든 정책 타입 포함)
    // 변경: POLICY, TERMS, PRIVACY 타입을 모두 포함하도록 IN 절 사용
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL AND n.noticeType IN ('POLICY', 'TERMS', 'PRIVACY') ORDER BY n.createdAt DESC")
    Page<Notice> findPolicies(Pageable pageable);

    /**
     * 완전 삭제 대상 조회 (purgeAfter가 현재 시간 이전)
     */
    @Query("SELECT n FROM Notice n WHERE n.status = 'PENDING_DELETE' AND n.purgeAfter IS NOT NULL AND n.purgeAfter <= :now")
    List<Notice> findPurgeTargets(@Param("now") LocalDateTime now);
}