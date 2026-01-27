package com.example.pproject.notification.service;

import com.example.pproject.notification.dto.NotificationCreateRequest;
import com.example.pproject.notification.dto.NotificationDTO;
import com.example.pproject.notification.entity.NotificationDelivery;
import com.example.pproject.notification.entity.NotificationTemplate;
import com.example.pproject.notification.repository.NotificationDeliveryRepository;
import com.example.pproject.notification.repository.NotificationTemplateRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 서비스
 * 
 * ========================================
 * ERD 테이블:
 * - notification_template (37번)
 * - notification_delivery (38번)
 * ========================================
 * 
 * 주요 기능:
 * - 알림 생성 및 발송
 * - 알림 목록 조회 (페이징)
 * - 알림 읽음 처리
 * - 읽지 않은 알림 수 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserRepository userRepository;

    // ==================== 알림 조회 ====================

    /**
     * 회원별 알림 목록 조회 (페이징)
     * 
     * @param memberId 회원 ID (member.member_id FK)
     * @param pageable 페이징 정보
     * @return 알림 목록 Page<NotificationDTO>
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(Long memberId, Pageable pageable) {
        log.debug("[알림] 회원 {} 알림 목록 조회, page: {}, size: {}", 
                memberId, pageable.getPageNumber(), pageable.getPageSize());
        
        return deliveryRepository.findByMemberId(memberId, pageable)
                .map(NotificationDTO::from);
    }

    /**
     * 최근 알림 조회 (드롭다운용)
     * 
     * @param memberId 회원 ID
     * @param limit 조회 개수
     * @return 최근 알림 목록
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getRecentNotifications(Long memberId, int limit) {
        log.debug("[알림] 회원 {} 최근 알림 조회, limit: {}", memberId, limit);
        
        return deliveryRepository.findRecentByMemberId(memberId, PageRequest.of(0, limit))
                .stream()
                .map(NotificationDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 읽지 않은 알림 수 조회
     * 
     * @param memberId 회원 ID
     * @return 읽지 않은 알림 수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        return deliveryRepository.countUnreadByMemberId(memberId);
    }

    // ==================== 알림 생성 ====================

    /**
     * 알림 생성 (Outbox 패턴에서 호출)
     * 
     * ERD 매핑:
     * - member_id    <- request.memberId (FK -> member.member_id)
     * - template_id  <- 템플릿 코드로 조회 (FK -> notification_template.template_id)
     * - event_type   <- request.eventType
     * - channel      <- request.channel
     * - payload      <- request.payload (JSONB)
     * - link_url     <- request.linkUrl
     * - status       <- 'PENDING' (DEFAULT)
     * - created_at   <- now() (DEFAULT)
     * 
     * @param request 알림 생성 요청
     * @return 생성된 알림 DTO
     */
    @Transactional
    public NotificationDTO createNotification(NotificationCreateRequest request) {
        log.info("[알림] 알림 생성 - memberId: {}, eventType: {}, channel: {}", 
                request.getMemberId(), request.getEventType(), request.getChannel());

        // 회원 조회 (member_id FK 검증)
        UserEntity member = userRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + request.getMemberId()));

        // 템플릿 조회 (optional, template_id FK)
        NotificationTemplate template = null;
        if (request.getTemplateCode() != null) {
            template = templateRepository.findByTemplateCode(request.getTemplateCode())
                    .orElse(null);
            if (template == null) {
                log.warn("[알림] 템플릿을 찾을 수 없음: {}", request.getTemplateCode());
            }
        }

        // 알림 발송 이력 생성
        NotificationDelivery delivery = NotificationDelivery.builder()
                .member(member)
                .template(template)
                .eventType(request.getEventType())
                .channel(request.getChannel())
                .payload(request.getPayload())
                .linkUrl(request.getLinkUrl())
                .build();

        NotificationDelivery saved = deliveryRepository.save(delivery);
        log.info("[알림] 알림 생성 완료 - deliveryId: {}", saved.getId());

        return NotificationDTO.from(saved);
    }

    /**
     * 간편 알림 생성 (PUSH 채널 기본)
     * 
     * @param memberId 회원 ID
     * @param eventType 이벤트 타입
     * @param payload 페이로드 JSON
     * @param linkUrl 링크 URL
     * @return 생성된 알림 DTO
     */
    @Transactional
    public NotificationDTO createPushNotification(Long memberId, String eventType, 
                                                   String payload, String linkUrl) {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .memberId(memberId)
                .eventType(eventType)
                .channel(NotificationTemplate.NotificationChannel.PUSH)
                .payload(payload)
                .linkUrl(linkUrl)
                .build();
        
        return createNotification(request);
    }

    // ==================== 알림 상태 변경 ====================

    /**
     * 특정 알림 읽음 처리
     * 
     * @param notificationId 알림 ID (delivery_id)
     * @param memberId 회원 ID (소유권 확인)
     */
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        log.debug("[알림] 읽음 처리 - notificationId: {}, memberId: {}", notificationId, memberId);
        
        NotificationDelivery delivery = deliveryRepository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));
        
        delivery.markAsRead();
        deliveryRepository.save(delivery);
    }

    /**
     * 모든 알림 읽음 처리
     * 
     * @param memberId 회원 ID
     * @return 읽음 처리된 알림 수
     */
    @Transactional
    public int markAllAsRead(Long memberId) {
        log.debug("[알림] 전체 읽음 처리 - memberId: {}", memberId);
        return deliveryRepository.markAllAsReadByMemberId(memberId);
    }

    /**
     * 알림 삭제
     * 
     * @param notificationId 알림 ID
     * @param memberId 회원 ID (소유권 확인)
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long memberId) {
        log.debug("[알림] 알림 삭제 - notificationId: {}, memberId: {}", notificationId, memberId);
        
        NotificationDelivery delivery = deliveryRepository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));
        
        deliveryRepository.delete(delivery);
    }

    // ==================== 템플릿 관리 ====================

    /**
     * 템플릿 조회
     * 
     * @param templateCode 템플릿 코드
     * @return 템플릿 Optional
     */
    @Transactional(readOnly = true)
    public NotificationTemplate getTemplate(String templateCode) {
        return templateRepository.findByTemplateCode(templateCode).orElse(null);
    }

    /**
     * 템플릿 생성/수정
     * 
     * @param template 템플릿 엔티티
     * @return 저장된 템플릿
     */
    @Transactional
    public NotificationTemplate saveTemplate(NotificationTemplate template) {
        return templateRepository.save(template);
    }
}
