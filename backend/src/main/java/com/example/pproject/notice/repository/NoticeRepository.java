package com.example.pproject.notice.repository;

import com.example.pproject.notice.model.Notice;
import com.example.pproject.notice.model.NoticeAttachment;
import com.example.pproject.notice.model.NoticeDelivery;
import com.example.pproject.Constant.NoticeType;
import com.example.pproject.Constant.NoticeStatus;
import com.example.pproject.Constant.DeliveryStatus;
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

    /**
     * 삭제되지 않은 공지사항 목록 조회 (페이지네이션)
     */
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    Page<Notice> findAllActive(Pageable pageable);

    /**
     * 공개된 공지사항만 조회 (회원용)
     */
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL AND n.isPublic = true AND n.status = 'ACTIVE' ORDER BY n.createdAt DESC")
    Page<Notice> findAllPublic(Pageable pageable);

    /**
     * 제목 또는 본문으로 공지사항 검색
     */
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL " +
            "AND (n.title LIKE %:keyword% OR n.body LIKE %:keyword%) " +
            "ORDER BY n.createdAt DESC")
    Page<Notice> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 공지사항 타입으로 필터링
     */
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL AND n.noticeType = :noticeType ORDER BY n.createdAt DESC")
    Page<Notice> findByNoticeType(@Param("noticeType") NoticeType noticeType, Pageable pageable);

    /**
     * ID로 삭제되지 않은 공지사항 조회
     */
    @Query("SELECT n FROM Notice n WHERE n.noticeId = :noticeId AND n.deletedAt IS NULL")
    Optional<Notice> findByIdActive(@Param("noticeId") Long noticeId);

    /**
     * 타입별 최신 활성 정책문서 조회
     * 동의 관리에 사용됨
     */
    @Query("SELECT n FROM Notice n WHERE n.noticeType = :noticeType AND n.status = 'ACTIVE' AND n.deletedAt IS NULL ORDER BY n.createdAt DESC LIMIT 1")
    Optional<Notice> findLatestActiveByType(@Param("noticeType") NoticeType noticeType);
}