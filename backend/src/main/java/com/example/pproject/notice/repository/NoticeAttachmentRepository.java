package com.example.pproject.notice.repository;

import com.example.pproject.notice.model.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    /**
     * 특정 공지사항의 모든 첨부파일 조회
     */
    @Query("SELECT na FROM NoticeAttachment na WHERE na.noticeId = :noticeId ORDER BY na.createdAt ASC")
    List<NoticeAttachment> findByNoticeId(@Param("noticeId") Long noticeId);

    /**
     * 특정 공지사항의 모든 첨부파일 삭제
     */
    void deleteByNoticeId(Long noticeId);
}

