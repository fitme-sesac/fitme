package com.example.pproject.notice.service;

import com.example.pproject.notice.dto.*;
import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.NoticeAttachment;
import com.example.pproject.notice.entity.NoticeDelivery; // [추가]
import com.example.pproject.notice.entity.Notice.NoticeStatus;
import com.example.pproject.notice.entity.Notice.NoticeType;
import com.example.pproject.notice.repository.NoticeDeliveryRepository; // [추가]
import com.example.pproject.notice.repository.NoticeRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeDeliveryRepository noticeDeliveryRepository; // [추가] 주입

    // ==================== ADMIN: 생성/수정/삭제 ====================

    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request, Long adminId) {
        // 정책 문서 유일성 체크
        if (Notice.isUniqueActivePolicyType(request.getNoticeType())) {
            noticeRepository.findActivePolicyForRotation(request.getNoticeType())
                    .ifPresent(Notice::markForDeletion);
        }

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

        if (request.getAttachments() != null) {
            for (NoticeCreateRequest.AttachmentRequest fileReq : request.getAttachments()) {
                NoticeAttachment attachment = NoticeAttachment.builder()
                        .fileUrl(fileReq.getFileUrl())
                        .fileName(fileReq.getFileName())
                        .build();
                notice.addAttachment(attachment);
            }
        }

        return NoticeResponse.from(noticeRepository.save(notice));
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
        noticeRepository.deleteById(noticeId);
    }

    // ==================== [추가] ADMIN: 공지 발송 ====================

    /**
     * 공지사항 발송 이력 생성 (ERD 32번 notice_delivery 테이블 대응)
     * 실제 발송 로직(메일/PUSH 등)은 별도 모듈이나 이벤트 리스너에서 처리한다고 가정하고,
     * 여기서는 DB에 '발송 준비(PENDING)' 상태의 이력을 남깁니다.
     */
    @Transactional
    public void sendNotice(Long noticeId, NoticeDelivery.DeliveryChannel channel, Long adminId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        // TODO: 실제로는 전체 회원(Member) 목록을 조회하여 Loop를 돌거나 Batch Insert를 해야 합니다.
        // 현재는 Member Repository 의존성이 없으므로, 예시로 '관리자 자신'에게 보내는 것으로 구현합니다.
        // 실무에서는: List<Long> targetMemberIds = memberRepository.findAllActiveIds();

        Long targetMemberId = adminId; // 테스트용: 발송 대상을 관리자 본인으로 설정

        NoticeDelivery delivery = NoticeDelivery.builder()
                .notice(notice)
                .memberId(targetMemberId)
                .channel(channel)
                .status(NoticeDelivery.DeliveryStatus.PENDING) // 초기 상태: 발송 대기
                .build();

        noticeDeliveryRepository.save(delivery);

        log.info("공지 발송 이력 생성 완료: noticeId={}, memberId={}, channel={}", noticeId, targetMemberId, channel);
    }

    // ==================== PUBLIC: 조회 ====================

    public Page<NoticeListResponse> getNoticesPublic(Pageable pageable) {
        return noticeRepository.findAllActivePublic(pageable).map(NoticeListResponse::from);
    }

    public Page<NoticeListResponse> getNoticesByTypePublic(NoticeType type, Pageable pageable) {
        return noticeRepository.findByTypePublic(type, pageable).map(NoticeListResponse::from);
    }

    public NoticeResponse getLatestPolicy(NoticeType type) {
        return noticeRepository.findActivePolicy(type)
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