package com.example.pproject.inquiry.service;

import com.example.pproject.inquiry.entity.Inquiry;
import com.example.pproject.inquiry.entity.InquiryMessage;
import com.example.pproject.inquiry.repository.InquiryRepository;
import com.example.pproject.inquiry.repository.InquiryMessageRepository;
import com.example.pproject.inquiry.dto.request.CreateInquiryRequest;
import com.example.pproject.inquiry.dto.request.ReplyInquiryRequest;
import com.example.pproject.inquiry.dto.request.UpdateInquiryStatusRequest;
import com.example.pproject.inquiry.dto.response.InquiryResponse;
import com.example.pproject.inquiry.dto.response.InquiryMessageResponse;
import com.example.pproject.inquiry.dto.response.InquiryListResponse;
import com.example.pproject.inquiry.exception.InquiryNotFoundException;
import com.example.pproject.inquiry.exception.InvalidInquiryStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryMessageRepository messageRepository;

    /**
     * 문의 생성
     */
    @Transactional
    public InquiryResponse createInquiry(CreateInquiryRequest request) {
        log.info("문의 생성 시작: memberId={}, title={}", request.getMemberId(), request.getTitle());

        Inquiry inquiry = new Inquiry();
        inquiry.setMemberId(request.getMemberId());
        inquiry.setTitle(request.getTitle());
        inquiry.setContent(request.getContent());
        inquiry.setCategory(request.getCategory());
        inquiry.setStatus("OPEN");

        Inquiry saved = inquiryRepository.save(inquiry);

        // 초기 메시지 (고객이 작성한 문의)
        InquiryMessage message = new InquiryMessage();
        message.setInquiryId(saved.getInquiryId());
        message.setAuthorType("CUSTOMER");
        message.setCustomerMemberId(request.getMemberId());
        message.setBody(request.getContent());
        messageRepository.save(message);

        log.info("문의 생성 완료: inquiryId={}", saved.getInquiryId());
        return toResponse(saved);
    }

    /**
     * 문의 조회 (상세)
     */
    public InquiryResponse getInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InquiryNotFoundException("문의를 찾을 수 없습니다. inquiryId=" + inquiryId));

        // 정렬된 메시지 목록 조회
        List<InquiryMessage> messages = messageRepository.findByInquiryIdOrderByCreatedAtAsc(inquiryId);

        InquiryResponse response = toResponse(inquiry);
        response.setMessageCount(messages.size());
        response.setMessages(
                messages.stream()
                        .map(this::toMessageResponse)
                        .collect(Collectors.toList())
        );

        return response;
    }

    /**
     * 회원별 문의 목록 조회
     */
    public Page<InquiryListResponse> getInquiriesByMember(Long memberId, Pageable pageable) {
        return inquiryRepository.findByMemberId(memberId, pageable)
                .map(this::toListResponse);
    }

    /**
     * 상태별 문의 조회 (관리자용)
     */
    public Page<InquiryListResponse> getInquiriesByStatus(String status, Pageable pageable) {
        validateStatus(status);
        return inquiryRepository.findByStatus(status, pageable)
                .map(this::toListResponse);
    }

    /**
     * 회원별 응답 대기 문의 개수
     */
    public long getOpenInquiryCount(Long memberId) {
        return inquiryRepository.countByMemberIdAndStatus(memberId, "OPEN");
    }

    /**
     * 문의에 답변 추가
     */
    @Transactional
    public InquiryResponse replyToInquiry(ReplyInquiryRequest request) {
        log.info("문의 답변 추가: inquiryId={}", request.getInquiryId());

        Inquiry inquiry = inquiryRepository.findById(request.getInquiryId())
                .orElseThrow(() -> new InquiryNotFoundException("문의를 찾을 수 없습니다."));

        // 답변 메시지 저장
        InquiryMessage message = new InquiryMessage();
        message.setInquiryId(request.getInquiryId());
        message.setAuthorType("ADMIN");
        message.setAdminMemberId(request.getAdminMemberId());
        message.setBody(request.getBody());
        messageRepository.save(message);

        // 문의 상태 업데이트
        inquiry.setStatus("ANSWERED");
        inquiry.setAnsweredAt(LocalDateTime.now());
        Inquiry updated = inquiryRepository.save(inquiry);

        log.info("문의 답변 추가 완료: inquiryId={}", request.getInquiryId());
        return getInquiry(updated.getInquiryId());
    }

    /**
     * 문의 상태 업데이트
     * [정책 반영]
     * 1. 답변 완료 후에는 관리자도 내용을 수정할 수 없음 (기존 구조 유지: Status만 변경 가능)
     * 2. 전자상거래법 제6조에 의거하여 종료(CLOSED) 시점부터 3년간 보관 설정
     */
    @Transactional
    public InquiryResponse updateInquiryStatus(UpdateInquiryStatusRequest request) {
        log.info("문의 상태 업데이트: inquiryId={}, status={}", request.getInquiryId(), request.getStatus());

        validateStatus(request.getStatus());

        Inquiry inquiry = inquiryRepository.findById(request.getInquiryId())
                .orElseThrow(() -> new InquiryNotFoundException("문의를 찾을 수 없습니다."));

        String previousStatus = inquiry.getStatus();
        inquiry.setStatus(request.getStatus());

        // ANSWERED 상태로 변경 시 답변 시간 기록
        if ("ANSWERED".equals(request.getStatus()) && inquiry.getAnsweredAt() == null) {
            inquiry.setAnsweredAt(LocalDateTime.now());
        }

        // ✅ [수정됨] CLOSED 상태로 변경 시 보관 기한 설정 (3년)
        if ("CLOSED".equals(request.getStatus())) {
            // 전자상거래법 제6조(소비자의 불만 또는 분쟁처리에 관한 기록): 3년
            inquiry.setRetentionUntil(LocalDateTime.now().plusYears(3));
        }

        Inquiry updated = inquiryRepository.save(inquiry);
        log.info("문의 상태 업데이트 완료: inquiryId={}, {} -> {}, 보관기한={}",
                request.getInquiryId(), previousStatus, request.getStatus(), inquiry.getRetentionUntil());

        return toResponse(updated);
    }

    /**
     * 문의 삭제 (고객용)
     */
    @Transactional
    public void deleteInquiry(Long inquiryId, Long memberId) {
        log.info("문의 삭제: inquiryId={}, memberId={}", inquiryId, memberId);

        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InquiryNotFoundException("문의를 찾을 수 없습니다."));

        // 본인의 문의만 삭제 가능
        if (!inquiry.getMemberId().equals(memberId)) {
            throw new InvalidInquiryStatusException("본인의 문의만 삭제할 수 있습니다.");
        }

        // 관련 메시지 삭제
        List<InquiryMessage> messages = messageRepository.findByInquiryIdOrderByCreatedAtAsc(inquiryId);
        messageRepository.deleteAll(messages);

        inquiryRepository.delete(inquiry);
        log.info("문의 삭제 완료: inquiryId={}", inquiryId);
    }

    /**
     * 상태 유효성 검증
     */
    private void validateStatus(String status) {
        if (!status.matches("^(OPEN|ANSWERED|CLOSED)$")) {
            throw new InvalidInquiryStatusException("유효하지 않은 상태입니다: " + status);
        }
    }

    /**
     * Inquiry Entity → InquiryResponse DTO 변환
     */
    private InquiryResponse toResponse(Inquiry inquiry) {
        return InquiryResponse.builder()
                .inquiryId(inquiry.getInquiryId())
                .memberId(inquiry.getMemberId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .category(inquiry.getCategory())
                .answeredAt(inquiry.getAnsweredAt())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }

    /**
     * Inquiry Entity → InquiryListResponse DTO 변환
     */
    private InquiryListResponse toListResponse(Inquiry inquiry) {
        long messageCount = messageRepository.countByInquiryId(inquiry.getInquiryId());

        return InquiryListResponse.builder()
                .inquiryId(inquiry.getInquiryId())
                .memberId(inquiry.getMemberId())
                .title(inquiry.getTitle())
                .status(inquiry.getStatus())
                .category(inquiry.getCategory())
                .messageCount((int) messageCount)
                .answeredAt(inquiry.getAnsweredAt())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }

    /**
     * InquiryMessage Entity → InquiryMessageResponse DTO 변환
     */
    private InquiryMessageResponse toMessageResponse(InquiryMessage message) {
        Long authorId = "CUSTOMER".equals(message.getAuthorType())
                ? message.getCustomerMemberId()
                : message.getAdminMemberId();

        String authorName = "CUSTOMER".equals(message.getAuthorType()) ? "고객" : "관리자";

        return InquiryMessageResponse.builder()
                .messageId(message.getMessageId())
                .inquiryId(message.getInquiryId())
                .authorType(message.getAuthorType())
                .authorId(authorId)
                .authorName(authorName)
                .body(message.getBody())
                .createdAt(message.getCreatedAt())
                .build();
    }
}