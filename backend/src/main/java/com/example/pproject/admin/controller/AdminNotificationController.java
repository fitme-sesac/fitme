package com.example.pproject.admin.controller;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class AdminNotificationController {

    @GetMapping
    public ResponseEntity<List<AdminNotificationResponse>> getNotifications() {
        // Mock Data
        return ResponseEntity.ok(Arrays.asList(
                AdminNotificationResponse.builder()
                        .id(1L)
                        .title("새로운 채용공고")
                        .message("네이버에서 새로운 공고를 등록했습니다.")
                        .type("JOB")
                        .createdAt(LocalDateTime.now().minusMinutes(10))
                        .isRead(false)
                        .build(),
                AdminNotificationResponse.builder()
                        .id(2L)
                        .title("신고 접수")
                        .message("부적절한 게시글 신고가 접수되었습니다.")
                        .type("REPORT")
                        .createdAt(LocalDateTime.now().minusHours(1))
                        .isRead(false)
                        .build(),
                AdminNotificationResponse.builder()
                        .id(3L)
                        .title("기업 가입 신청")
                        .message("(주)피트미 기업 가입 승인 대기중입니다.")
                        .type("COMPANY")
                        .createdAt(LocalDateTime.now().minusHours(2))
                        .isRead(true)
                        .build()));
    }

    @Getter
    @Builder
    public static class AdminNotificationResponse {
        private Long id;
        private String title;
        private String message;
        private String type;
        private LocalDateTime createdAt;
        private boolean isRead;
    }
}
