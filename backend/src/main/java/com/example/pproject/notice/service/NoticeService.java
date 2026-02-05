package com.example.pproject.notice.service;

import com.example.pproject.notice.dto.*;
import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.NoticeAttachment;
import com.example.pproject.notice.entity.NoticeDelivery;
import com.example.pproject.notice.entity.Notice.NoticeStatus;
import com.example.pproject.notice.entity.Notice.NoticeType;
import com.example.pproject.notice.repository.NoticeDeliveryRepository;
import com.example.pproject.notice.repository.NoticeRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeDeliveryRepository noticeDeliveryRepository;

    // ==================== ADMIN: 생성/수정/삭제 ====================

    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request, Long adminId) {
        // TERMS/PRIVACY/POLICY: 타입당 1건만 유지. 기존 행이 있으면 UPDATE만 하고 INSERT 하지 않음 (notice_id 시퀀스 중복 방지)
        if (Notice.isUniqueActivePolicyType(request.getNoticeType())) {
            Optional<Notice> existingOpt = noticeRepository.findTopByNoticeTypeOrderByCreatedAtDesc(request.getNoticeType());
            if (existingOpt.isPresent()) {
                Notice existing = existingOpt.get();
                existing.update(
                        request.getTitle(),
                        request.getBody(),
                        request.getIsPublic() != null ? request.getIsPublic() : true,
                        request.getNoticeType(),
                        NoticeStatus.ACTIVE,
                        request.getPurgeAfter(),
                        adminId
                );
                Notice savedNotice = noticeRepository.save(existing);
                if (savedNotice.getIsPublic()) {
                    sendNotice(savedNotice.getId(), NoticeDelivery.DeliveryChannel.EMAIL, adminId);
                    log.info("중요 약관 갱신으로 인한 자동 알림 발송 트리거 완료: noticeId={}", savedNotice.getId());
                }
                return NoticeResponse.from(savedNotice);
            }
        }

        // OPS 또는 정책 타입이지만 기존 행 없음: 새 공지 INSERT
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .noticeType(request.getNoticeType())
                .status(NoticeStatus.ACTIVE)
                .purgeAfter(request.getPurgeAfter())
                .createdBy(adminId)
                .updatedBy(adminId)
                .build();

        if (request.getAttachments() != null) {
            for (NoticeCreateRequest.AttachmentRequest fileReq : request.getAttachments()) {
                NoticeAttachment attachment = NoticeAttachment.builder()
                        .fileUrl(fileReq.getFileUrl())
                        .fileName(fileReq.getFileName())
                        .build();
                notice.addAttachment(attachment);
            }
        }

        Notice savedNotice = noticeRepository.save(notice);

        if (Notice.isUniqueActivePolicyType(savedNotice.getNoticeType()) && savedNotice.getIsPublic()) {
            sendNotice(savedNotice.getId(), NoticeDelivery.DeliveryChannel.EMAIL, adminId);
            log.info("중요 약관 등록으로 인한 자동 알림 발송 트리거 완료: noticeId={}", savedNotice.getId());
        }

        return NoticeResponse.from(savedNotice);
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request, Long adminId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        notice.update(
                request.getTitle(),
                request.getBody(),
                request.getIsPublic(),
                request.getNoticeType(),
                request.getStatus(),
                request.getPurgeAfter(),
                adminId
        );
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        notice.markForDeletion();

        notice.update(
                notice.getTitle(), notice.getBody(), false, notice.getNoticeType(),
                NoticeStatus.PENDING_DELETE,
                LocalDateTime.now().plusDays(30),
                notice.getUpdatedBy()
        );

        log.info("공지 삭제 요청(30일 유예): noticeId={}", noticeId);
    }

    // ==================== ADMIN: 공지 발송 ====================

    @Transactional
    public void sendNotice(Long noticeId, NoticeDelivery.DeliveryChannel channel, Long adminId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        Long targetMemberId = adminId;

        NoticeDelivery delivery = NoticeDelivery.builder()
                .notice(notice)
                .memberId(targetMemberId)
                .channel(channel)
                .status(NoticeDelivery.DeliveryStatus.PENDING)
                .build();

        noticeDeliveryRepository.save(delivery);
    }

    // ==================== PUBLIC: 조회 ====================

    public Page<NoticeListResponse> getNoticesPublic(Pageable pageable) {
        return noticeRepository.findAllActivePublic(pageable).map(NoticeListResponse::from);
    }

    public Page<NoticeListResponse> getNoticesByTypePublic(NoticeType type, Pageable pageable) {
        return noticeRepository.findByTypePublic(type, pageable).map(NoticeListResponse::from);
    }

    /**
     * ✅ [핵심] 최신 정책 1건 조회
     * 기존 에러 원인: 데이터가 18개인데 1개만 달라고 해서 발생.
     * 수정: findTop... 메서드를 사용하여 가장 최신 1개만 가져오도록 변경.
     */
    public NoticeResponse getLatestPolicy(NoticeType type) {
        return noticeRepository.findTopByNoticeTypeAndStatusAndIsPublicOrderByCreatedAtDesc(
                        type,
                        NoticeStatus.ACTIVE,
                        true
                )
                .map(NoticeResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("현재 활성화된 정책 문서가 없습니다."));
    }

    public NoticeResponse getNoticePublic(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .filter(n -> n.getIsPublic() && n.getStatus() == NoticeStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("찾을 수 없거나 비공개된 공지사항입니다."));
        return NoticeResponse.from(notice);
    }

    public Page<NoticeListResponse> searchNoticesPublic(String keyword, Pageable pageable) {
        return noticeRepository.searchPublic(keyword, pageable).map(NoticeListResponse::from);
    }

    // ==================== ADMIN: 조회 및 통계 ====================

    public NoticeResponse getNoticeAdmin(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .map(NoticeResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));
    }

    public Page<NoticeListResponse> getNoticesAdmin(Pageable pageable, NoticeType noticeType) {
        if (noticeType != null) {
            return noticeRepository.findByNoticeType(noticeType, pageable).map(NoticeListResponse::from);
        }
        return noticeRepository.findAll(pageable).map(NoticeListResponse::from);
    }

    public NoticeStatistics getStatistics() {
        long activeTotal = noticeRepository.countActive();
        Map<NoticeType, Long> countMap = new HashMap<>();
        List<Object[]> results = noticeRepository.countNoticesGroupByType();

        for (Object[] row : results) {
            countMap.put((NoticeType) row[0], (Long) row[1]);
        }

        for (NoticeType type : NoticeType.values()) {
            countMap.putIfAbsent(type, 0L);
        }

        return new NoticeStatistics(activeTotal, countMap);
    }

    @Getter
    public static class NoticeStatistics {
        private final long activeTotalCount;
        private final Map<NoticeType, Long> totalCountByType;

        public NoticeStatistics(long activeTotalCount, Map<NoticeType, Long> totalCountByType) {
            this.activeTotalCount = activeTotalCount;
            this.totalCountByType = totalCountByType;
        }
    }
}