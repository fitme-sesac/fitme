package com.example.pproject.notification.service;

import com.example.pproject.Constant.NotificationChannel;
import com.example.pproject.Constant.NotificationDeliveryStatus;
import com.example.pproject.Constant.NotificationEventType;
import com.example.pproject.notification.dto.CreateNotificationRequest;
import com.example.pproject.notification.dto.NotificationCountResponse;
import com.example.pproject.notification.dto.NotificationListResponse;
import com.example.pproject.notification.dto.NotificationResponse;
import com.example.pproject.notification.entity.NotificationDelivery;
import com.example.pproject.notification.repository.NotificationDeliveryRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 알림 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final UserRepository userRepository;

    /**
     * 알림 생성
     */
    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        UserEntity member = userRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + request.getMemberId()));

        // payload에 title, message, linkUrl 포함
        Map<String, Object> payload = new HashMap<>();
        if (request.getPayload() != null) {
            payload.putAll(request.getPayload());
        }
        payload.put("title", request.getTitle());
        payload.put("message", request.getMessage());
        payload.put("linkUrl", request.getLinkUrl());

        NotificationDelivery notification = NotificationDelivery.builder()
                .member(member)
                .eventType(request.getEventType())
                .channel(request.getChannel() != null ? request.getChannel() : NotificationChannel.PUSH)
                .payload(payload)
                .status(NotificationDeliveryStatus.SENT)
                .build();

        notification.markAsSent();
        NotificationDelivery saved = notificationDeliveryRepository.save(notification);

        log.info("알림 생성 완료: memberId={}, eventType={}, notificationId={}",
                request.getMemberId(), request.getEventType(), saved.getId());

        return NotificationResponse.from(saved);
    }

    /**
     * 간편 알림 생성 (이벤트 타입 기반 기본 메시지 사용)
     */
    @Transactional
    public NotificationResponse createNotification(
            Integer memberId,
            NotificationEventType eventType,
            String linkUrl,
            Map<String, Object> payload
    ) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .memberId(memberId)
                .eventType(eventType)
                .channel(NotificationChannel.PUSH)
                .title(getDefaultTitle(eventType))
                .message(eventType.getDefaultMessage())
                .linkUrl(linkUrl)
                .payload(payload)
                .build();

        return createNotification(request);
    }

    /**
     * 커스텀 메시지로 알림 생성
     */
    @Transactional
    public NotificationResponse createNotification(
            Integer memberId,
            NotificationEventType eventType,
            String title,
            String message,
            String linkUrl,
            Map<String, Object> payload
    ) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .memberId(memberId)
                .eventType(eventType)
                .channel(NotificationChannel.PUSH)
                .title(title)
                .message(message)
                .linkUrl(linkUrl)
                .payload(payload)
                .build();

        return createNotification(request);
    }

    /**
     * 회원의 알림 목록 조회 (페이징)
     */
    public NotificationListResponse getNotifications(Integer memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDelivery> notificationPage = notificationDeliveryRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId, pageable);

        List<NotificationResponse> notifications = notificationPage.getContent().stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());

        long unreadCount = notificationDeliveryRepository.countUnreadByMemberId(memberId);

        return NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .totalCount(notificationPage.getTotalElements())
                .page(page)
                .size(size)
                .hasNext(notificationPage.hasNext())
                .build();
    }

    /**
     * 회원의 최근 알림 목록 조회 (드롭다운용)
     */
    public List<NotificationResponse> getRecentNotifications(Integer memberId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationDeliveryRepository.findRecentByMemberId(memberId, pageable).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 읽지 않은 알림 수 조회
     */
    public NotificationCountResponse getUnreadCount(Integer memberId) {
        long count = notificationDeliveryRepository.countUnreadByMemberId(memberId);
        return NotificationCountResponse.builder()
                .unreadCount(count)
                .build();
    }

    /**
     * 특정 알림 읽음 처리
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Integer memberId) {
        NotificationDelivery notification = notificationDeliveryRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));

        if (!notification.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
        }

        if (!notification.isRead()) {
            notification.markAsRead();
            notificationDeliveryRepository.save(notification);
            log.info("알림 읽음 처리: notificationId={}, memberId={}", notificationId, memberId);
        }
        
        return NotificationResponse.from(notification);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public int markAllAsRead(Integer memberId) {
        List<NotificationDelivery> notifications = notificationDeliveryRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId);
        
        int count = 0;
        for (NotificationDelivery notification : notifications) {
            if (!notification.isRead()) {
                notification.markAsRead();
                notificationDeliveryRepository.save(notification);
                count++;
            }
        }
        
        log.info("모든 알림 읽음 처리: memberId={}, count={}", memberId, count);
        return count;
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, Integer memberId) {
        NotificationDelivery notification = notificationDeliveryRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));

        if (!notification.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림만 삭제할 수 있습니다.");
        }

        notificationDeliveryRepository.delete(notification);
        log.info("알림 삭제: notificationId={}, memberId={}", notificationId, memberId);
    }

    /**
     * 이벤트 타입에 따른 기본 제목 반환
     */
    private String getDefaultTitle(NotificationEventType eventType) {
        return switch (eventType) {
            case APPLICATION_SUBMITTED -> "지원 완료";
            case APPLICATION_VIEWED -> "이력서 열람";
            case APPLICATION_STATUS_CHANGED -> "지원 상태 변경";
            case INTERVIEW_SCHEDULED -> "면접 일정 확정";
            case INTERVIEW_REMINDER -> "면접 일정 알림";
            case INTERVIEW_CANCELED -> "면접 취소";
            case INTERVIEW_RESULT -> "면접 결과";
            case HIRED -> "합격 축하";
            case REJECTED -> "전형 결과";
            case PAYMENT_COMPLETED -> "결제 완료";
            case PAYMENT_FAILED -> "결제 실패";
            case PAYMENT_REFUNDED -> "환불 완료";
            case SUBSCRIPTION_STARTED -> "구독 시작";
            case SUBSCRIPTION_RENEWED -> "구독 갱신";
            case SUBSCRIPTION_EXPIRING -> "구독 만료 예정";
            case SUBSCRIPTION_CANCELED -> "구독 취소";
            case CREDIT_CHARGED -> "크레딧 충전";
            case CREDIT_USED -> "크레딧 사용";
            case CREDIT_LOW -> "크레딧 부족";
            case NEW_APPLICATION_RECEIVED -> "새 지원자";
            case JOB_POSTING_APPROVED -> "공고 승인";
            case JOB_POSTING_REJECTED -> "공고 반려";
            case JOB_POSTING_EXPIRED -> "공고 만료";
            case PROFILE_UPDATED -> "회원정보 수정";
            case PASSWORD_CHANGED -> "비밀번호 변경";
            case SYSTEM_NOTICE -> "시스템 공지";
        };
    }
}
