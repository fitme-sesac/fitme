package com.example.pproject.notice.repository;

import com.example.pproject.notice.entity.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    /**
     * 공지사항별 첨부파일 조회
     */
    List<NoticeAttachment> findByNoticeId(Long noticeId);

    /**
     * 첨부파일 삭제 (공지사항별)
     */
    void deleteByNoticeId(Long noticeId);
}
