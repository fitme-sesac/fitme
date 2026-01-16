package com.example.pproject.faq.service;

import com.example.pproject.faq.dto.FaqCreateRequest;
import com.example.pproject.faq.dto.FaqListResponse;
import com.example.pproject.faq.dto.FaqResponse;
import com.example.pproject.faq.dto.FaqUpdateRequest;
import com.example.pproject.faq.entity.Faq;
import com.example.pproject.faq.repository.FaqRepository;
import com.example.pproject.global.exception.BusinessException;
import com.example.pproject.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FaqService {

    private final FaqRepository faqRepository;

    /**
     * ADM-FAQ-001: FAQ 목록 조회
     * 공개 여부 및 키워드로 검색 가능
     */
    @Transactional(readOnly = true)
    public Page<FaqListResponse> getFaqList(Boolean isPublic, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Faq> faqs;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드 검색
            if (isPublic != null) {
                faqs = faqRepository.searchPublicByKeyword(keyword, pageable);
            } else {
                faqs = faqRepository.searchByKeyword(keyword, pageable);
            }
        } else {
            // 필터링 없이 조회
            if (isPublic != null) {
                faqs = faqRepository.findByIsPublicAndNotDeleted(isPublic, pageable);
            } else {
                faqs = faqRepository.findAllNotDeleted(pageable);
            }
        }

        log.info("FAQ 목록 조회 - 공개여부: {}, 키워드: {}, 페이지: {}", isPublic, keyword, page);

        return faqs.map(FaqListResponse::fromEntity);
    }

    /**
     * ADM-FAQ-002: FAQ 신규 등록
     */
    @Transactional
    public FaqResponse createFaq(FaqCreateRequest request) {
        // 중복 질문 검증
        if (faqRepository.existsByQuestionAndNotDeleted(request.getQuestion())) {
            log.warn("중복된 FAQ 질문 등록 시도: {}", request.getQuestion());
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 질문입니다");
        }

        Faq faq = request.toEntity();
        Faq savedFaq = faqRepository.save(faq);

        log.info("FAQ 생성 완료 - ID: {}", savedFaq.getId());

        return FaqResponse.fromEntity(savedFaq);
    }

    /**
     * ADM-FAQ-003: FAQ 수정
     */
    @Transactional
    public FaqResponse updateFaq(Long id, FaqUpdateRequest request) {
        Faq faq = faqRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 FAQ 수정 시도 - ID: {}", id);
                    return new BusinessException(ErrorCode.NOT_FOUND, "FAQ를 찾을 수 없습니다");
                });

        // 다른 FAQ와 중복된 질문인 경우 검증
        if (!faq.getQuestion().equals(request.getQuestion()) &&
                faqRepository.existsByQuestionAndNotDeleted(request.getQuestion())) {
            log.warn("중복된 FAQ 질문으로 수정 시도: {}", request.getQuestion());
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 질문입니다");
        }

        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setIsPublic(request.getIsPublic());

        if (request.getLocked() != null) {
            faq.setLocked(request.getLocked());
        }

        Faq updatedFaq = faqRepository.save(faq);

        log.info("FAQ 수정 완료 - ID: {}", updatedFaq.getId());

        return FaqResponse.fromEntity(updatedFaq);
    }

    /**
     * ADM-FAQ-004: FAQ 삭제 (소프트 삭제)
     * 논리 삭제를 위해 deleted_at 필드 업데이트
     */
    @Transactional
    public void deleteFaq(Long id) {
        Faq faq = faqRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 FAQ 삭제 시도 - ID: {}", id);
                    return new BusinessException(ErrorCode.NOT_FOUND, "FAQ를 찾을 수 없습니다");
                });

        faq.delete();
        faqRepository.save(faq);

        log.info("FAQ 삭제 완료 - ID: {}", id);
    }

    /**
     * FAQ 상세 조회
     */
    @Transactional(readOnly = true)
    public FaqResponse getFaqDetail(Long id) {
        Faq faq = faqRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 FAQ 상세 조회 시도 - ID: {}", id);
                    return new BusinessException(ErrorCode.NOT_FOUND, "FAQ를 찾을 수 없습니다");
                });

        return FaqResponse.fromEntity(faq);
    }

    /**
     * 공개 FAQ 목록 조회 (사용자용)
     */
    @Transactional(readOnly = true)
    public List<FaqResponse> getPublicFaqList() {
        List<Faq> faqs = faqRepository.findAllPublicFaqs();

        log.info("공개 FAQ 목록 조회 - 총 {}개", faqs.size());

        return faqs.stream()
                .map(FaqResponse::fromEntity)
                .collect(Collectors.toList());
    }
}