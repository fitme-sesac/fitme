package com.example.pproject.notice.service;

import com.example.pproject.notice.dto.*;
import com.example.pproject.notice.model.Notice;
import com.example.pproject.notice.model.NoticeAttachment;
import com.example.pproject.notice.repository.NoticeRepository;
import com.example.pproject.notice.repository.NoticeAttachmentRepository;
import com.example.pproject.Constant.NoticeType;
import com.example.pproject.Constant.NoticeStatus;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeAttachmentRepository noticeAttachmentRepository;

    /**
     * 공지사항 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<NoticeResponse> getNoticeList(String keyword, String noticeType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notice> result;

        if (keyword != null && !keyword.isEmpty()) {
            result = noticeRepository.searchByKeyword(keyword, pageable);
        } else if (noticeType != null && !noticeType.isEmpty()) {
            NoticeType type = NoticeType.from(noticeType);
            result = noticeRepository.findByNoticeType(type, pageable);
        } else {
            result = noticeRepository.findAllActive(pageable);
        }

        return result.map(this::toNoticeResponse);
    }

    /**
     * 공개된 공지사항 목록 조회 (회원용)
     */
    @Transactional(readOnly = true)
    public Page<NoticeResponse> getPublicNoticeList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return noticeRepository.findAllPublic(pageable).map(this::toNoticeResponse);
    }

    /**
     * 공지사항 상세 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public NoticeResponse getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findByIdActive(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("공지사항을 찾을 수 없습니다. ID: " + noticeId));
        return toNoticeResponse(notice);
    }

    /**
     * 공지사항 신규 등록 (관리자용)
     */
    public NoticeResponse createNotice(CreateNoticeRequest request, Long adminId) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .isPublic(request.getIsPublic())
                .noticeType(NoticeType.from(request.getNoticeType()))
                .status(request.getStatus() != null ? NoticeStatus.from(request.getStatus()) : NoticeStatus.ACTIVE)
                .createdBy(adminId)
                .updatedBy(adminId)
                .build();

        Notice savedNotice = noticeRepository.save(notice);
        log.info("공지사항 생성: ID={}, 제목={}", savedNotice.getNoticeId(), savedNotice.getTitle());

        return toNoticeResponse(savedNotice);
    }

    /**
     * 공지사항 수정 (관리자용)
     */
    public NoticeResponse updateNotice(Long noticeId, UpdateNoticeRequest request, Long adminId) {
        Notice notice = noticeRepository.findByIdActive(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        notice.setTitle(request.getTitle());
        notice.setBody(request.getBody());
        notice.setIsPublic(request.getIsPublic());
        notice.setUpdatedBy(adminId);

        Notice updatedNotice = noticeRepository.save(notice);
        log.info("공지사항 수정: ID={}, 제목={}", updatedNotice.getNoticeId(), updatedNotice.getTitle());

        return toNoticeResponse(updatedNotice);
    }

    /**
     * 공지사항 삭제 (관리자용) - 논리 삭제
     */
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdActive(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        notice.softDelete();
        noticeRepository.save(notice);
        log.info("공지사항 삭제: ID={}, 제목={}", noticeId, notice.getTitle());
    }

    /**
     * 첨부파일 추가 (관리자용)
     */
    public AttachmentResponse addAttachment(Long noticeId, AddAttachmentRequest request) {
        Notice notice = noticeRepository.findByIdActive(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        NoticeAttachment attachment = NoticeAttachment.builder()
                .noticeId(noticeId)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .build();

        NoticeAttachment savedAttachment = noticeAttachmentRepository.save(attachment);
        log.info("첨부파일 추가: ID={}, 공지ID={}, 파일명={}",
                savedAttachment.getAttachmentId(), noticeId, request.getFileName());

        return AttachmentResponse.builder()
                .attachmentId(savedAttachment.getAttachmentId())
                .fileUrl(savedAttachment.getFileUrl())
                .fileName(savedAttachment.getFileName())
                .createdAt(savedAttachment.getCreatedAt())
                .build();
    }

    /**
     * 첨부파일 제거 (관리자용)
     */
    public void removeAttachment(Long attachmentId) {
        noticeAttachmentRepository.deleteById(attachmentId);
        log.info("첨부파일 제거: ID={}", attachmentId);
    }

    /**
     * Entity → DTO 변환
     */
    private NoticeResponse toNoticeResponse(Notice notice) {
        // 해당 공지사항의 첨부파일 조회
        java.util.List<AttachmentResponse> attachments = noticeAttachmentRepository
                .findByNoticeId(notice.getNoticeId())
                .stream()
                .map(a -> AttachmentResponse.builder()
                        .attachmentId(a.getAttachmentId())
                        .fileUrl(a.getFileUrl())
                        .fileName(a.getFileName())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return NoticeResponse.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .body(notice.getBody())
                .isPublic(notice.getIsPublic())
                .noticeType(notice.getNoticeType().name())
                .status(notice.getStatus().name())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .attachments(attachments)
                .build();
    }
}