package com.example.pproject.faq.service;

import com.example.pproject.faq.dto.CreateFaqRequest;
import com.example.pproject.faq.dto.FaqResponse;
import com.example.pproject.faq.dto.UpdateFaqRequest;
import com.example.pproject.faq.model.Faq;
import com.example.pproject.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FaqService {

    private final FaqRepository faqRepository;

    /**
     * FAQ 목록 조회 (관리자용)
     * - 모든 FAQ 조회 (삭제되지 않은 것만)
     * - 키워드 검색 가능
     * - 공개/비공개 필터링 가능
     */
    @Transactional(readOnly = true)
    public Page<FaqResponse> getFaqList(String keyword, Boolean isPublic, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Faq> result;

        if (keyword != null && !keyword.isEmpty()) {
            // 키워드 검색
            result = faqRepository.searchByKeyword(keyword, pageable);
        } else if (isPublic != null) {
            // 공개/비공개 필터링
            result = faqRepository.findByIsPublic(isPublic, pageable);
        } else {
            // 전체 조회
            result = faqRepository.findAllActive(pageable);
        }

        return result.map(this::toFaqResponse);
    }

    /**
     * FAQ 목록 조회 (회원용)
     * - 공개된 FAQ만 조회
     * - 삭제되지 않은 것만 조회
     */
    @Transactional(readOnly = true)
    public Page<FaqResponse> getPublicFaqList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return faqRepository.findAllPublic(pageable).map(this::toFaqResponse);
    }

    /**
     * FAQ 상세 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public FaqResponse getFaqDetail(Long faqId) {
        Faq faq = faqRepository.findByIdActive(faqId)
                .orElseThrow(() -> new EntityNotFoundException("FAQ를 찾을 수 없습니다. ID: " + faqId));
        return toFaqResponse(faq);
    }

    /**
     * FAQ 신규 등록 (관리자용)
     */
    public FaqResponse createFaq(CreateFaqRequest request) {
        Faq faq = Faq.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .isPublic(request.getIsPublic())
                .locked(request.getLocked() != null ? request.getLocked() : true)
                .build();

        Faq savedFaq = faqRepository.save(faq);
        log.info("FAQ 생성: ID={}, 질문={}", savedFaq.getFaqId(), savedFaq.getQuestion());

        return toFaqResponse(savedFaq);
    }

    /**
     * FAQ 수정 (관리자용)
     */
    public FaqResponse updateFaq(Long faqId, UpdateFaqRequest request) {
        Faq faq = faqRepository.findByIdActive(faqId)
                .orElseThrow(() -> new EntityNotFoundException("FAQ를 찾을 수 없습니다. ID: " + faqId));

        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setIsPublic(request.getIsPublic());
        faq.setLocked(request.getLocked() != null ? request.getLocked() : true);

        Faq updatedFaq = faqRepository.save(faq);
        log.info("FAQ 수정: ID={}, 질문={}", updatedFaq.getFaqId(), updatedFaq.getQuestion());

        return toFaqResponse(updatedFaq);
    }

    /**
     * FAQ 삭제 (관리자용)
     * - 논리 삭제 (deleted_at 업데이트)
     */
    public void deleteFaq(Long faqId) {
        Faq faq = faqRepository.findByIdActive(faqId)
                .orElseThrow(() -> new EntityNotFoundException("FAQ를 찾을 수 없습니다. ID: " + faqId));

        faq.softDelete();
        faqRepository.save(faq);
        log.info("FAQ 삭제: ID={}, 질문={}", faqId, faq.getQuestion());
    }

    /**
     * Entity → DTO 변환
     */
    private FaqResponse toFaqResponse(Faq faq) {
        return FaqResponse.builder()
                .faqId(faq.getFaqId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .locked(faq.getLocked())
                .isPublic(faq.getIsPublic())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}