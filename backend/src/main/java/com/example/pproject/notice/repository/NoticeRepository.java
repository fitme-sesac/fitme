package com.example.pproject.notice.repository;

import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.Notice.NoticeType;
import com.example.pproject.notice.entity.Notice.NoticeStatus; // ✅ Enum 임포트 필수!
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

    // ==================== [1] 일반 사용자용 조회 (공개 + 활성 상태) ====================

    /**
     * 활성 공지 전체 목록 조회 (공개된 것만)
     * 👉 [에러 해결] 이 메서드가 없어서 에러가 났었습니다.
     */
    @Query("SELECT n FROM Notice n WHERE n.status = 'ACTIVE' AND n.isPublic = true ORDER BY n.createdAt DESC")
    Page<Notice> findAllActivePublic(Pageable pageable);

    /**
     * 특정 타입의 활성 공지 목록 (공개된 것만)
     */
    @Query("SELECT n FROM Notice n WHERE n.noticeType = :type AND n.status = 'ACTIVE' AND n.isPublic = true ORDER BY n.createdAt DESC")
    Page<Notice> findByTypePublic(@Param("type") NoticeType type, Pageable pageable);

    /**
     * 제목+본문 검색 (공개된 것만)
     */
    @Query("SELECT n FROM Notice n WHERE n.status = 'ACTIVE' AND n.isPublic = true AND (n.title LIKE %:keyword% OR n.body LIKE %:keyword%) ORDER BY n.createdAt DESC")
    Page<Notice> searchPublic(@Param("keyword") String keyword, Pageable pageable);


    // ==================== [2] 정책/약관 조회 (단건) ====================

    /**
     * [단건 조회용] 데이터가 많아도 가장 최신 것 '1개'만 안전하게 가져옴
     * 👉 findTop + OrderByCreatedAtDesc 사용
     */
    Optional<Notice> findTopByNoticeTypeAndStatusAndIsPublicOrderByCreatedAtDesc(
            NoticeType noticeType,
            NoticeStatus status, // ✅ Enum 타입 사용
            boolean isPublic
    );


    // ==================== [3] 관리자용 조회 (상태 무관) ====================

    /**
     * 제목+본문 검색 (관리자용 - 모든 상태 조회)
     */
    @Query("SELECT n FROM Notice n WHERE n.title LIKE %:keyword% OR n.body LIKE %:keyword% ORDER BY n.createdAt DESC")
    Page<Notice> searchAll(@Param("keyword") String keyword, Pageable pageable);

    /**
     * [약관 교체용] 현재 'ACTIVE' 상태인 정책 조회 (내부 로직용)
     */
    @Query("SELECT n FROM Notice n WHERE n.noticeType = :type AND n.status = 'ACTIVE'")
    Optional<Notice> findActivePolicyForRotation(@Param("type") NoticeType type);


    // ==================== [4] 통계용 ====================

    @Query("SELECT COUNT(n) FROM Notice n WHERE n.status = 'ACTIVE'")
    long countActive();

    /**
     * 타입별 게시물 수 조회
     */
    @Query("SELECT n.noticeType, COUNT(n) FROM Notice n GROUP BY n.noticeType")
    List<Object[]> countNoticesGroupByType();
}