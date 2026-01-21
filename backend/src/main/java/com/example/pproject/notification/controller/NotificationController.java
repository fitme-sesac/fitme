package com.example.pproject.notification.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.global.response.ApiResponse;
import com.example.pproject.notification.dto.NotificationCountResponse;
import com.example.pproject.notification.dto.NotificationListResponse;
import com.example.pproject.notification.dto.NotificationResponse;
import com.example.pproject.notification.service.NotificationService;
import com.example.pproject.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 API 컨트롤러
 */
@Slf4j
@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * 알림 목록 조회 (페이징)
     */
    @Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 알림 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Integer memberId = getMemberIdFromPrincipal(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("4001", "로그인이 필요합니다."));
        }
        
        NotificationListResponse response = notificationService.getNotifications(memberId, page, size);
        return ResponseEntity.ok(ApiResponse.success("알림 목록 조회 성공", response));
    }

    /**
     * 최근 알림 조회 (드롭다운용)
     */
    @Operation(summary = "최근 알림 조회", description = "드롭다운 표시용 최근 알림을 조회합니다.")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getRecentNotifications(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Integer memberId = getMemberIdFromPrincipal(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("4001", "로그인이 필요합니다."));
        }
        
        List<NotificationResponse> response = notificationService.getRecentNotifications(memberId, limit);
        return ResponseEntity.ok(ApiResponse.success("최근 알림 조회 성공", response));
    }

    /**
     * 읽지 않은 알림 수 조회
     */
    @Operation(summary = "읽지 않은 알림 수 조회", description = "읽지 않은 알림의 개수를 조회합니다.")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationCountResponse>> getUnreadCount(
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        Integer memberId = getMemberIdFromPrincipal(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("4001", "로그인이 필요합니다."));
        }
        
        NotificationCountResponse response = notificationService.getUnreadCount(memberId);
        return ResponseEntity.ok(ApiResponse.success("읽지 않은 알림 수 조회 성공", response));
    }

    /**
     * 특정 알림 읽음 처리
     */
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long notificationId
    ) {
        Integer memberId = getMemberIdFromPrincipal(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("4001", "로그인이 필요합니다."));
        }
        
        NotificationResponse response = notificationService.markAsRead(notificationId, memberId);
        return ResponseEntity.ok(ApiResponse.success("알림 읽음 처리 성공", response));
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Operation(summary = "모든 알림 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        Integer memberId = getMemberIdFromPrincipal(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("4001", "로그인이 필요합니다."));
        }
        
        int count = notificationService.markAllAsRead(memberId);
        return ResponseEntity.ok(ApiResponse.success("모든 알림 읽음 처리 성공", count));
    }

    /**
     * 알림 삭제
     */
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long notificationId
    ) {
        Integer memberId = getMemberIdFromPrincipal(principal);
        if (memberId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("4001", "로그인이 필요합니다."));
        }
        
        notificationService.deleteNotification(notificationId, memberId);
        return ResponseEntity.ok(ApiResponse.success("알림 삭제 성공", null));
    }

    /**
     * JWT Principal에서 memberId 추출
     */
    private Integer getMemberIdFromPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.getUserid() == null) {
            return null;
        }
        
        try {
            return userRepository.findByUserid(principal.getUserid())
                    .map(user -> user.getId())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("memberId 조회 실패: {}", e.getMessage());
            return null;
        }
    }
}
