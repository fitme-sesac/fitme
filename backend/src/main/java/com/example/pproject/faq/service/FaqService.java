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
     * Retrieve a paginated list of active FAQs for administrative use, optionally filtered by keyword or public visibility.
     *
     * If `keyword` is non-empty, performs a keyword search; otherwise if `isPublic` is non-null, filters by public visibility;
     * otherwise returns all active (not deleted) FAQs. Results are sorted by `createdAt` descending.
     *
     * @param keyword an optional search term to match FAQ content; treated as not provided when null or empty
     * @param isPublic an optional visibility filter; when non-null, only FAQs with matching `isPublic` are returned
     * @param page zero-based page index
     * @param size number of items per page
     * @return a page of {@code FaqResponse} objects representing the matching FAQs
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
     * Retrieve a paginated list of publicly visible, non-deleted FAQs for members.
     *
     * @param page zero-based page index
     * @param size the number of items per page
     * @return a page of FaqResponse objects representing public, active FAQs sorted by creation time descending
     */
    @Transactional(readOnly = true)
    public Page<FaqResponse> getPublicFaqList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return faqRepository.findAllPublic(pageable).map(this::toFaqResponse);
    }

    /**
     * Retrieve detailed information for an active FAQ by its ID for administrative use.
     *
     * @param faqId the ID of the FAQ to retrieve
     * @return a FaqResponse containing the FAQ's details
     * @throws javax.persistence.EntityNotFoundException if no active FAQ exists with the given ID
     */
    @Transactional(readOnly = true)
    public FaqResponse getFaqDetail(Long faqId) {
        Faq faq = faqRepository.findByIdActive(faqId)
                .orElseThrow(() -> new EntityNotFoundException("FAQ를 찾을 수 없습니다. ID: " + faqId));
        return toFaqResponse(faq);
    }

    /**
     * Create a new FAQ from the provided request data.
     *
     * If `locked` is not specified in the request, the FAQ is created with `locked = true`.
     *
     * @param request the DTO containing question, answer, visibility, and optional locked flag
     * @return the persisted FAQ as a FaqResponse
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
     * Update an existing FAQ using values from the provided request.
     *
     * The request's `locked` field defaults to `true` when null.
     *
     * @param faqId  the identifier of the active FAQ to update
     * @param request  the payload containing updated `question`, `answer`, `isPublic`, and optional `locked`
     * @return the updated FAQ represented as a `FaqResponse`
     * @throws EntityNotFoundException if no active FAQ exists with the given `faqId`
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
     * Soft-delete an active FAQ identified by its ID for administrative use.
     *
     * Updates the entity to a deleted state (sets the deletion timestamp) and persists the change.
     *
     * @param faqId the ID of the FAQ to soft-delete
     * @throws javax.persistence.EntityNotFoundException if no active FAQ exists with the given ID
     */
    public void deleteFaq(Long faqId) {
        Faq faq = faqRepository.findByIdActive(faqId)
                .orElseThrow(() -> new EntityNotFoundException("FAQ를 찾을 수 없습니다. ID: " + faqId));

        faq.softDelete();
        faqRepository.save(faq);
        log.info("FAQ 삭제: ID={}, 질문={}", faqId, faq.getQuestion());
    }

    /**
     * Convert a Faq entity into a FaqResponse DTO.
     *
     * @param faq the source Faq entity
     * @return a FaqResponse containing faqId, question, answer, locked, isPublic, createdAt, and updatedAt
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