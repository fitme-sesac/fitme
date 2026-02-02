package com.example.pproject.notification.repository;

import com.example.pproject.notification.entity.NotificationTemplate;
import com.example.pproject.notification.entity.NotificationTemplate.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 템플릿 Repository
 * 
 * ========================================
 * ERD 테이블: notification_template (37번)
 * ========================================
 * 
 * 주요 인덱스:
 * - PK: template_id
 * - UNIQUE: template_code
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * 템플릿 코드로 조회
     * - UNIQUE 인덱스 활용
     * 
     * @param templateCode 템플릿 코드 (예: APPLICATION_SUBMITTED)
     * @return 템플릿 Optional
     */
    Optional<NotificationTemplate> findByTemplateCode(String templateCode);

    /**
     * 채널별 템플릿 목록 조회
     * 
     * @param channel 발송 채널 (EMAIL/SMS/PUSH)
     * @return 해당 채널의 템플릿 목록
     */
    List<NotificationTemplate> findByChannel(NotificationChannel channel);

    /**
     * 템플릿 코드 존재 여부 확인
     * 
     * @param templateCode 템플릿 코드
     * @return 존재 여부
     */
    boolean existsByTemplateCode(String templateCode);

    /**
     * 템플릿 코드와 채널로 조회
     * 
     * @param templateCode 템플릿 코드
     * @param channel 발송 채널
     * @return 템플릿 Optional
     */
    Optional<NotificationTemplate> findByTemplateCodeAndChannel(String templateCode, NotificationChannel channel);
}
