package com.example.pproject.notification.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.notification.dto.NotificationDTO;
import com.example.pproject.notification.dto.UnreadCountResponse;
import com.example.pproject.notification.service.NotificationService;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 알림 컨트롤러
 * 
 * ========================================
 * ERD 테이블: notification_delivery (38번)
 * ========================================
 * 
 * API 엔드포인트:
 * - GET  /api/notifications           : 알림 목록 조회 (페이징)
 * - GET  /api/notifications/recent    : 최근 알림 조회 (드롭다운용)
 * - GET  /api/notifications/unread-count : 읽지 않은 알림 수
 * - PATCH /api/notifications/{id}/read : 특정 알림 읽음 처리
 * - PATCH /api/notifications/read-all : 모든 알림 읽음 처리
 * - DELETE /api/notifications/{id}    : 알림 삭제
 * 
 * 인증 필요: 모든 API는 로그인 필수
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * 알림 목록 조회 (페이징)
     * 
     * @param principal JWT 인증 정보
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 알림 목록 Page
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long memberId = getMemberId(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다"
            ));
        }

        Page<NotificationDTO> notifications = notificationService.getNotifications(
                memberId, 
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", notifications.getContent(),
                "page", Map.of(
                        "number", notifications.getNumber(),
                        "size", notifications.getSize(),
                        "totalElements", notifications.getTotalElements(),
                        "totalPages", notifications.getTotalPages(),
                        "hasNext", notifications.hasNext()
                )
        ));
    }

    /**
     * 최근 알림 조회 (드롭다운용)
     * 
     * @param principal JWT 인증 정보
     * @param limit 조회 개수 (기본값: 10)
     * @return 최근 알림 목록
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentNotifications(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit) {
        
        Long memberId = getMemberId(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다"
            ));
        }

        List<NotificationDTO> notifications = notificationService.getRecentNotifications(memberId, limit);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", notifications
        ));
    }

    /**
     * 읽지 않은 알림 수 조회
     * 
     * @param principal JWT 인증 정보
     * @return 읽지 않은 알림 수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        Long memberId = getMemberId(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다"
            ));
        }

        long unreadCount = notificationService.getUnreadCount(memberId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", UnreadCountResponse.of(unreadCount)
        ));
    }

    /**
     * 특정 알림 읽음 처리
     * 
     * @param principal JWT 인증 정보
     * @param notificationId 알림 ID (delivery_id)
     * @return 성공 응답
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long notificationId) {
        
        Long memberId = getMemberId(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다"
            ));
        }

        try {
            notificationService.markAsRead(notificationId, memberId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "알림을 읽음 처리했습니다"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 모든 알림 읽음 처리
     * 
     * @param principal JWT 인증 정보
     * @return 읽음 처리된 알림 수
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        
        Long memberId = getMemberId(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다"
            ));
        }

        int count = notificationService.markAllAsRead(memberId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", count + "개의 알림을 읽음 처리했습니다",
                "data", Map.of("updatedCount", count)
        ));
    }

    /**
     * 알림 삭제
     * 
     * @param principal JWT 인증 정보
     * @param notificationId 알림 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long notificationId) {
        
        Long memberId = getMemberId(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다"
            ));
        }

        try {
            notificationService.deleteNotification(notificationId, memberId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "알림을 삭제했습니다"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ==================== Helper Methods ====================

    /**
     * JWT Principal에서 회원 ID 추출
     * - userid(login_id) 또는 email로 회원 조회
     */
    private Long getMemberId(JwtUserPrincipal principal) {
        if (principal == null) {
            return null;
        }

        String userid = principal.getUserid();
        if (userid == null) {
            return null;
        }

        // 1. login_id로 조회
        var userOpt = userRepository.findByUserid(userid);
        if (userOpt.isPresent()) {
            return userOpt.get().getId();
        }

        // 2. email로 조회 (소셜 로그인)
        if (userid.contains("@")) {
            userOpt = userRepository.findByEmail(userid);
            if (userOpt.isPresent()) {
                return userOpt.get().getId();
            }
        }

        return null;
    }
}
