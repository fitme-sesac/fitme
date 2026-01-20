package com.example.pproject.notice.service;

import com.example.pproject.global.exception.BusinessException;
import com.example.pproject.global.exception.ErrorCode;
import com.example.pproject.notice.dto.*;
import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.NoticeAttachment;
import com.example.pproject.notice.entity.NoticeDelivery;
import com.example.pproject.notice.repository.NoticeAttachmentRepository;
import com.example.pproject.notice.repository.NoticeDeliveryRepository;
import com.example.pproject.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeAttachmentRepository attachmentRepository;
    private final NoticeDeliveryRepository deliveryRepository;

    /**
     * ADM-NTC-001: 공지 등록
     */
    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request) {
        Notice notice = request.toEntity();
        Notice savedNotice = noticeRepository.save(notice);

        log.info("공지사항 생성 완료 - ID: {}, 제목: {}", savedNotice.getId(), savedNotice.getTitle());

        return NoticeResponse.fromEntity(savedNotice);
    }

    /**
     * ADM-NTC-002: 첨부파일 업로드
     */
    @Transactional
    public NoticeAttachmentResponse uploadAttachment(Long noticeId, NoticeAttachmentRequest request) {
        Notice notice = noticeRepository.findByIdAndNotDeleted(noticeId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 공지사항 첨부파일 업로드 시도 - ID: {}", noticeId);
                    return new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다");
                });

        NoticeAttachment attachment = request.toEntity();
        notice.addAttachment(attachment);
        NoticeAttachment savedAttachment = attachmentRepository.save(attachment);

        log.info("공지사항 첨부파일 업로드 완료 - 공지ID: {}, 파일: {}", noticeId, savedAttachment.getFileName());

        return NoticeAttachmentResponse.fromEntity(savedAttachment);
    }

    /**
     * ADM-NTC-003: 공지 수정/삭제
     * 삭제 시 purge_after를 현재+30일로 설정
     */
    @Transactional
    public NoticeResponse updateNotice(Long id, NoticeUpdateRequest request) {
        Notice notice = noticeRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 공지사항 수정 시도 - ID: {}", id);
                    return new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다");
                });

        // 제목 수정
        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            notice.setTitle(request.getTitle());
        }

        // 본문 수정
        if (request.getBody() != null && !request.getBody().isEmpty()) {
            notice.setBody(request.getBody());
        }

        // 타입 수정
        if (request.getNoticeType() != null) {
            notice.setNoticeType(Notice.NoticeType.valueOf(request.getNoticeType()));
        }

        // 중요도 수정
        if (request.getIsImportant() != null) {
            notice.setIsImportant(request.getIsImportant());
        }

        // 공개 여부 수정
        if (request.getIsPublic() != null) {
            notice.setIsPublic(request.getIsPublic());
        }

        // 상태 변경 (삭제)
        if (request.getStatus() != null && "PENDING_DELETE".equals(request.getStatus())) {
            notice.delete();
        }

        Notice updatedNotice = noticeRepository.save(notice);

        log.info("공지사항 수정 완료 - ID: {}", updatedNotice.getId());

        return NoticeResponse.fromEntity(updatedNotice);
    }

    /**
     * ADM-NTC-004: 공지 개별 발송
     * 특정 회원 그룹에게 이메일이나 SMS로 발송
     */
    @Transactional
    public void deliverNotice(Long noticeId, NoticeDeliveryRequest request) {
        Notice notice = noticeRepository.findByIdAndNotDeleted(noticeId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 공지사항 발송 시도 - ID: {}", noticeId);
                    return new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다");
                });

        NoticeDelivery.DeliveryChannel channel = NoticeDelivery.DeliveryChannel.valueOf(request.getChannel());

        // 각 회원별 발송 기록 생성
        for (Long memberId : request.getTargetMemberIds()) {
            NoticeDelivery delivery = NoticeDelivery.builder()
                    .notice(notice)
                    .memberId(memberId)
                    .channel(channel)
                    .status(NoticeDelivery.DeliveryStatus.PENDING)
                    .build();

            notice.addDelivery(delivery);
            deliveryRepository.save(delivery);

            // 실제 환경에서는 여기서 이메일/SMS 발송 로직 호출
            // sendNotification(memberId, channel, notice);

            // 시뮬레이션: 발송 성공 처리
            delivery.setStatus(NoticeDelivery.DeliveryStatus.SENT);
            delivery.setDeliveredAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
        }

        log.info("공지사항 발송 완료 - ID: {}, 채널: {}, 대상 인원: {}",
                noticeId, channel, request.getTargetMemberIds().size());
    }

    /**
     * ADM-NTC-005: 발송 결과 조회
     */
    @Transactional(readOnly = true)
    public Page<NoticeDeliveryResponse> getDeliveryResults(Long noticeId, int page, int size) {
        // 공지사항 존재 여부 확인
        noticeRepository.findByIdAndNotDeleted(noticeId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 공지사항 발송 결과 조회 시도 - ID: {}", noticeId);
                    return new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다");
                });

        Pageable pageable = PageRequest.of(page, size);

        log.info("공지사항 발송 결과 조회 - ID: {}, 페이지: {}", noticeId, page);

        return deliveryRepository.findByNoticeId(noticeId, pageable)
                .map(NoticeDeliveryResponse::fromEntity);
    }

    /**
     * ADM-POL-001: 정책동의서 등록
     * 수정사항: 이용약관/개인정보방침 등 타입 허용 및 중복 활성 정책 검증
     */
    @Transactional
    public NoticeResponse createPolicy(NoticeCreateRequest request) {
        // 1. 입력된 타입이 정책 관련 타입인지 검증 (POLICY, TERMS, PRIVACY 등)
        Notice.NoticeType type = Notice.NoticeType.valueOf(request.getNoticeType());
        if (type == Notice.NoticeType.OPS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "운영 공지(OPS)는 정책으로 등록할 수 없습니다.");
        }

        // 2. [핵심] 해당 타입의 'ACTIVE' 정책이 이미 존재하는지 확인
        boolean existsActive = noticeRepository.existsByNoticeTypeAndStatus(type, Notice.NoticeStatus.ACTIVE);
        if (existsActive) {
            // 방법 A: 에러 발생 (관리자가 기존 것을 먼저 내리게 유도) - 추천
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 활성화된 해당 타입의 정책이 존재합니다. 기존 정책을 비활성화해주세요.");

            // 방법 B: 기존 정책을 자동으로 INACTIVE 처리하려면 여기서 별도 로직 구현 필요
        }

        Notice notice = request.toEntity();
        notice.setIsPublic(true); // 정책은 무조건 공개
        Notice savedNotice = noticeRepository.save(notice);

        log.info("정책동의서 생성 완료 - ID: {}, 타입: {}, 제목: {}",
                savedNotice.getId(), type, savedNotice.getTitle());

        return NoticeResponse.fromEntity(savedNotice);
    }


    /**
     * ADM-POL-002: 정책 동의서 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getPolicies(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        log.info("정책 동의서 목록 조회 - 페이지: {}", page);

        return noticeRepository.findPolicies(pageable)
                .map(NoticeListResponse::fromEntity);
    }

    /**
     * 공지사항 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getNoticeList(String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notice> notices;

        if (type != null && !type.isEmpty()) {
            notices = noticeRepository.findByNoticeType(Notice.NoticeType.valueOf(type), pageable);
        } else {
            notices = noticeRepository.findAllActive(pageable);
        }

        log.info("공지사항 목록 조회 - 타입: {}, 페이지: {}", type, page);

        return notices.map(NoticeListResponse::fromEntity);
    }

    /**
     * 공지사항 상세 조회
     */
    @Transactional(readOnly = true)
    public NoticeResponse getNoticeDetail(Long id) {
        Notice notice = noticeRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 공지사항 상세 조회 시도 - ID: {}", id);
                    return new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다");
                });

        return NoticeResponse.fromEntity(notice);
    }

    /**
     * 공개 공지사항 목록 (사용자용)
     */
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getPublicNoticeList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        log.info("공개 공지사항 목록 조회 - 페이지: {}", page);

        return noticeRepository.findAllPublic(pageable)
                .map(NoticeListResponse::fromEntity);
    }

    /**
     * 중요 공지사항 조회
     */
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> getImportantNotices(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        log.info("중요 공지사항 조회 - 페이지: {}", page);

        return noticeRepository.findImportantNotices(pageable)
                .map(NoticeListResponse::fromEntity);
    }

    /**
     * 공지사항 검색 (제목)
     */
    @Transactional(readOnly = true)
    public Page<NoticeListResponse> searchNotices(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        log.info("공지사항 검색 - 키워드: {}, 페이지: {}", keyword, page);

        return noticeRepository.searchByTitle(keyword, pageable)
                .map(NoticeListResponse::fromEntity);
    }

    /**
     * 완전 삭제 대상 공지사항 자동 삭제 (배치 작업)
     */
    @Transactional
    public void purgeExpiredNotices() {
        List<Notice> expiredNotices = noticeRepository.findPurgeTargets(LocalDateTime.now());

        for (Notice notice : expiredNotices) {
            notice.setStatus(Notice.NoticeStatus.DELETED);
            attachmentRepository.deleteByNoticeId(notice.getId());
            noticeRepository.save(notice);

            log.info("공지사항 완전 삭제 - ID: {}", notice.getId());
        }

        log.info("완료된 공지사항 자동 삭제 - 처리 건수: {}", expiredNotices.size());
    }
}