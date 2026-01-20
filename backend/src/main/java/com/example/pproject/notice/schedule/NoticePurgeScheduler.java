package com.example.pproject.notice.schedule;

import com.example.pproject.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Notice 배치 작업
 * 매일 자정에 완전 삭제 대상 공지사항을 삭제합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NoticePurgeScheduler {

    private final NoticeService noticeService;

    /**
     * 매일 자정 (00:00:00)에 실행
     * 삭제 후 30일이 경과한 공지사항을 완전 삭제합니다.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void purgeExpiredNotices() {
        log.info("공지사항 자동 삭제 배치 작업 시작");

        try {
            noticeService.purgeExpiredNotices();
            log.info("공지사항 자동 삭제 배치 작업 완료");
        } catch (Exception e) {
            log.error("공지사항 자동 삭제 배치 작업 실패", e);
        }
    }
}