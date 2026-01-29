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
        // 1. 정책 문서 Rotation (기존 활성 약관 처리)
        if (Notice.isUniqueActivePolicyType(request.getNoticeType())) {
            // 주의: 만약 DB에 이미 중복된 Active 정책이 여러 개 있다면 여기서 에러가 날 수 있습니다.
            // (Repository의 findActivePolicyForRotation이 Optional을 반환하기 때문)
            // 테스트 중 발생한 중복 데이터는 DB에서 직접 정리하거나, 로직을 List 조회로 변경해야 합니다.
            noticeRepository.findActivePolicyForRotation(request.getNoticeType())
                    .ifPresent(existingNotice -> {
                        existingNotice.markForDeletion();
                        log.info("기존 약관(id={}) 상태 변경 -> PENDING_DELETE", existingNotice.getId());
                    });
        }

        // 2. 공지 생성
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .isPublic(request.getIsPublic())
                .noticeType(request.getNoticeType())
                .status(NoticeStatus.ACTIVE)
                .purgeAfter(request.getPurgeAfter())
                .createdBy(adminId)
                .updatedBy(adminId)
                .build();

        // 3. 첨부파일 처리
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

        // 4. 자동 알림 발송 트리거
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

    public Page<NoticeListResponse> getNoticesAdmin(Pageable pageable) {
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