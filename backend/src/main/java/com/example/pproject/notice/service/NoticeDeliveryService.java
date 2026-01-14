package com.example.pproject.notice.service;

import com.example.pproject.notice.dto.BatchDeliveryRequest;
import com.example.pproject.notice.dto.DeliveryResultResponse;
import com.example.pproject.notice.model.Notice;
import com.example.pproject.notice.model.NoticeDelivery;
import com.example.pproject.notice.repository.NoticeRepository;
import com.example.pproject.notice.repository.NoticeDeliveryRepository;
import com.example.pproject.Constant.DeliveryChannel;
import com.example.pproject.Constant.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NoticeDeliveryService {

    private final NoticeDeliveryRepository noticeDeliveryRepository;
    private final NoticeRepository noticeRepository;

    /**
     * 공지사항을 여러 회원에게 발송
     * 대량 발송을 위한 발송 기록 생성
     */
    public void sendNoticeToMembers(Long noticeId, BatchDeliveryRequest request) {
        Notice notice = noticeRepository.findByIdActive(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        // 각 회원별 발송 기록 생성
        List<NoticeDelivery> deliveries = request.getTargetMemberIds()
                .stream()
                .map(memberId -> NoticeDelivery.builder()
                        .noticeId(noticeId)
                        .memberId(memberId)
                        .channel(DeliveryChannel.from(request.getChannel()))
                        .status(DeliveryStatus.PENDING)
                        .build())
                .collect(Collectors.toList());

        noticeDeliveryRepository.saveAll(deliveries);
        log.info("대량 발송 생성: 공지ID={}, 채널={}, 회원수={}",
                noticeId, request.getChannel(), request.getTargetMemberIds().size());
    }

    /**
     * 공지사항의 발송 결과 조회
     */
    @Transactional(readOnly = true)
    public Page<DeliveryResultResponse> getDeliveryResults(Long noticeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return noticeDeliveryRepository.findByNoticeId(noticeId, pageable)
                .map(this::toDeliveryResultResponse);
    }

    /**
     * 발송 완료 표시
     */
    public void markAsSent(Long deliveryId) {
        NoticeDelivery delivery = noticeDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("발송 기록을 찾을 수 없습니다. ID: " + deliveryId));

        delivery.setStatus(DeliveryStatus.SENT);
        delivery.setSentAt(LocalDateTime.now());
        noticeDeliveryRepository.save(delivery);
        log.info("발송 완료 표시: ID={}", deliveryId);
    }

    /**
     * 발송 실패 표시
     */
    public void markAsFailed(Long deliveryId, String failReason) {
        NoticeDelivery delivery = noticeDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("발송 기록을 찾을 수 없습니다. ID: " + deliveryId));

        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailReason(failReason);
        noticeDeliveryRepository.save(delivery);
        log.info("발송 실패 표시: ID={}, 사유={}", deliveryId, failReason);
    }

    /**
     * 대기 중인 발송 건 조회 (배치 처리용)
     */
    @Transactional(readOnly = true)
    public List<NoticeDelivery> getPendingDeliveries() {
        return noticeDeliveryRepository.findPending();
    }

    /**
     * Entity → DTO 변환
     */
    private DeliveryResultResponse toDeliveryResultResponse(NoticeDelivery delivery) {
        return DeliveryResultResponse.builder()
                .deliveryId(delivery.getDeliveryId())
                .noticeId(delivery.getNoticeId())
                .memberId(delivery.getMemberId())
                .channel(delivery.getChannel())
                .status(delivery.getStatus())
                .failReason(delivery.getFailReason())
                .sentAt(delivery.getSentAt())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}