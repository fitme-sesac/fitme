package com.example.pproject.faq.service;

import com.example.pproject.faq.dto.FAQCreateRequest;
import com.example.pproject.faq.dto.FAQDetailResponse;
import com.example.pproject.faq.dto.FAQListResponse;
import com.example.pproject.faq.dto.FAQUpdateRequest;
import com.example.pproject.faq.dto.FAQResponse;
import com.example.pproject.faq.entity.FAQ;
import com.example.pproject.faq.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FAQService {

    private final FAQRepository faqRepository;

    // ... (createFAQ, getFAQPublic 등 기존 조회 메서드는 변경 없음) ...
    // ... createFAQ ~ updateFAQ까지는 기존 코드 유지 ...

    public FAQResponse createFAQ(FAQCreateRequest request) {
        if (faqRepository.existsByQuestion(request.getQuestion())) {
            throw new IllegalArgumentException("이미 존재하는 질문입니다");
        }
        FAQ faq = request.toEntity();
        FAQ savedFAQ = faqRepository.save(faq);
        return FAQResponse.from(savedFAQ);
    }

    @Transactional(readOnly = true)
    public FAQDetailResponse getFAQPublic(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));
        if (!faq.getIsPublic()) {
            throw new IllegalArgumentException("비공개 처리된 FAQ입니다");
        }
        return FAQDetailResponse.from(faq);
    }

    @Transactional(readOnly = true)
    public FAQResponse getFAQAdmin(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));
        return FAQResponse.from(faq);
    }

    @Transactional(readOnly = true)
    public Page<FAQListResponse> getFAQsPublic(Pageable pageable) {
        return faqRepository.findAllPublic(pageable).map(FAQListResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FAQResponse> getFAQsAdmin(Pageable pageable) {
        return faqRepository.findAllAdmin(pageable).map(FAQResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FAQListResponse> getFAQsByPublicStatus(Boolean isPublic, Pageable pageable) {
        return faqRepository.findByIsPublic(isPublic, pageable).map(FAQListResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FAQListResponse> getFAQsByLockStatus(Boolean locked, Pageable pageable) {
        return faqRepository.findByLocked(locked, pageable).map(FAQListResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FAQListResponse> getFAQsByPublicAndLock(Boolean isPublic, Boolean locked, Pageable pageable) {
        return faqRepository.findByIsPublicAndLocked(isPublic, locked, pageable).map(FAQListResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FAQListResponse> searchFAQsPublic(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getFAQsPublic(pageable);
        }
        return faqRepository.searchPublic(keyword.trim(), pageable).map(FAQListResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FAQDetailResponse> searchFAQsPublicFull(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getFAQsPublic(pageable).map(response ->
                    FAQDetailResponse.builder()
                            .id(response.getId())
                            .question(response.getQuestion())
                            .createdAt(response.getCreatedAt())
                            .build()
            );
        }
        return faqRepository.searchPublicFull(keyword.trim(), pageable).map(FAQDetailResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FAQResponse> searchFAQsAdmin(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getFAQsAdmin(pageable);
        }
        return faqRepository.searchAll(keyword.trim(), pageable).map(FAQResponse::from);
    }

    @Transactional(readOnly = true)
    public List<FAQListResponse> getRecentFAQs(int limit) {
        return faqRepository.findRecentPublic(limit).stream()
                .map(FAQListResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FAQDetailResponse> getRecentlyUpdatedFAQs(int limit) {
        return faqRepository.findRecentlyUpdatedPublic(limit).stream()
                .map(FAQDetailResponse::from)
                .collect(Collectors.toList());
    }

    public FAQResponse updateFAQ(Long faqId, FAQUpdateRequest request) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        if (!faq.getQuestion().equals(request.getQuestion()) &&
                faqRepository.existsByQuestion(request.getQuestion())) {
            throw new IllegalArgumentException("이미 존재하는 질문입니다");
        }

        faq.update(
                request.getQuestion(),
                request.getAnswer(),
                request.getLocked() != null ? request.getLocked() : faq.getLocked(),
                request.getIsPublic()
        );

        FAQ updatedFAQ = faqRepository.save(faq);
        return FAQResponse.from(updatedFAQ);
    }

    public FAQResponse togglePublic(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));
        faq.setPublic(!faq.getIsPublic());
        return FAQResponse.from(faq);
    }

    public FAQResponse toggleLock(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));
        faq.toggleLock();
        return FAQResponse.from(faq);
    }

    public FAQResponse lockFAQ(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));
        if (faq.getLocked()) throw new IllegalArgumentException("이미 잠금 상태입니다");
        faq.lock();
        return FAQResponse.from(faq);
    }

    public FAQResponse unlockFAQ(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));
        if (!faq.getLocked()) throw new IllegalArgumentException("잠금 상태가 아닙니다");
        faq.unlock();
        return FAQResponse.from(faq);
    }

    public void bulkUpdatePublic(List<Long> faqIds, Boolean isPublic) {
        List<FAQ> faqs = faqRepository.findAllById(faqIds);
        if (faqs.size() != faqIds.size()) {
            throw new IllegalArgumentException("요청하신 ID 중 존재하지 않는 FAQ가 포함되어 있습니다.");
        }
        faqRepository.bulkUpdatePublic(faqIds, isPublic);
    }

    public void bulkUpdateLocked(List<Long> faqIds, Boolean locked) {
        List<FAQ> faqs = faqRepository.findAllById(faqIds);
        if (faqs.size() != faqIds.size()) {
            throw new IllegalArgumentException("요청하신 ID 중 존재하지 않는 FAQ가 포함되어 있습니다.");
        }
        faqRepository.bulkUpdateLocked(faqIds, locked);
    }

    // ==================== DELETE (수정됨) ====================

    /**
     * FAQ 삭제 (Soft Delete)
     * Entity의 @SQLDelete 설정에 의해 자동으로 Update 쿼리가 실행됩니다.
     */
    public void deleteFAQ(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        // 이미 삭제된 경우 확인 (Entity에 @Where가 있어서 조회조차 안 될 수 있지만, 안전장치)
        if (faq.isDeleted()) {
            throw new IllegalArgumentException("이미 삭제된 FAQ입니다.");
        }

        faqRepository.delete(faq); // -> UPDATE faq SET deleted_at = now()... 실행됨
        log.info("FAQ soft deleted: id={}", faqId);
    }

    /**
     * 여러 FAQ 삭제 (Soft Delete 최적화)
     */
    public void bulkDeleteFAQs(List<Long> faqIds) {
        List<FAQ> faqs = faqRepository.findAllById(faqIds);

        if (faqs.size() != faqIds.size()) {
            throw new IllegalArgumentException("요청하신 ID 중 존재하지 않는 FAQ가 포함되어 있습니다.");
        }

        // JPA deleteAll -> 각 엔티티의 delete 호출 -> @SQLDelete 실행
        faqRepository.deleteAll(faqs);

        log.info("Bulk soft deleted {} FAQs", faqIds.size());
    }

    // ==================== Utility Methods ====================

    @Transactional(readOnly = true)
    public FAQStatistics getStatistics() {
        return FAQStatistics.builder()
                .totalCount(faqRepository.countAll())
                .publicCount(faqRepository.countPublic())
                .lockedCount(faqRepository.countLocked())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean hasPublicFAQs() {
        return faqRepository.existsPublic();
    }

    @lombok.Getter
    @lombok.Builder
    public static class FAQStatistics {
        private Long totalCount;
        private Long publicCount;
        private Long lockedCount;
    }
}