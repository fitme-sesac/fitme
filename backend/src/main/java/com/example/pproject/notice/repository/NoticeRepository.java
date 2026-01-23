package com.example.pproject.notice.repository;

import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.Notice.NoticeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // ==================== 기본 조회 ====================

    /**
     * 활성 공지만 조회 (공개된 것만) - 일반 사용자용
     */
    @Query("SELECT n FROM Notice n WHERE n.status = 'ACTIVE' AND n.isPublic = true ORDER BY n.createdAt DESC")
    Page<Notice> findAllActivePublic(Pageable pageable);

    // ==================== 검색 ====================

    /**
     * 제목+본문 검색 (공개된 것만) - 일반 사용자용
     */
    @Query("SELECT n FROM Notice n WHERE n.status = 'ACTIVE' AND n.isPublic = true AND (n.title LIKE %:keyword% OR n.body LIKE %:keyword%) ORDER BY n.createdAt DESC")
    Page<Notice> searchPublic(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 제목+본문 검색 (관리자용 - 상태/공개여부 무관)
     */
    @Query("SELECT n FROM Notice n WHERE n.title LIKE %:keyword% OR n.body LIKE %:keyword% ORDER BY n.createdAt DESC")
    Page<Notice> searchAll(@Param("keyword") String keyword, Pageable pageable);

    // ==================== 정책 및 타입별 조회 ====================

    /**
     * 특정 타입의 활성 공지 목록 (공개된 것만)
     */
    @Query("SELECT n FROM Notice n WHERE n.noticeType = :type AND n.status = 'ACTIVE' AND n.isPublic = true ORDER BY n.createdAt DESC")
    Page<Notice> findByTypePublic(@Param("type") NoticeType type, Pageable pageable);

    /**
     * [약관 조회용] 특정 타입의 유일한 활성 정책 (공개된 것만) - 일반 사용자용
     */
    @Query("SELECT n FROM Notice n WHERE n.noticeType = :type AND n.status = 'ACTIVE' AND n.isPublic = true")
    Optional<Notice> findActivePolicy(@Param("type") NoticeType type);

    /**
     * [약관 교체용 - 중요] 공개 여부와 상관없이 현재 'ACTIVE' 상태인 정책 조회
     * 이유: DB 유니크 제약조건(uq_notice_active_policy_per_type) 충돌 방지를 위해
     * 새 약관 등록 전 반드시 기존 ACTIVE 건을 찾아 내려야 함.
     */
    @Query("SELECT n FROM Notice n WHERE n.noticeType = :type AND n.status = 'ACTIVE'")
    Optional<Notice> findActivePolicyForRotation(@Param("type") NoticeType type);

    // ==================== 통계용 (최적화) ====================

    @Query("SELECT COUNT(n) FROM Notice n WHERE n.status = 'ACTIVE'")
    long countActive();

    /**
     * 타입별 게시물 수 조회 (GROUP BY 최적화)
     * 결과 예: [[OPS, 10], [TERMS, 1], ...]
     */
    @Query("SELECT n.noticeType, COUNT(n) FROM Notice n GROUP BY n.noticeType")
    List<Object[]> countNoticesGroupByType();
}