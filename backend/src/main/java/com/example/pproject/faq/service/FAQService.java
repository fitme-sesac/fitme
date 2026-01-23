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

    // ==================== CREATE ====================

    /**
     * 새 FAQ 생성
     */
    public FAQResponse createFAQ(FAQCreateRequest request) {
        // 중복 질문 체크
        if (faqRepository.existsByQuestion(request.getQuestion())) {
            throw new IllegalArgumentException("이미 존재하는 질문입니다");
        }

        FAQ faq = request.toEntity();
        FAQ savedFAQ = faqRepository.save(faq);

        log.info("FAQ created: id={}, question={}", savedFAQ.getId(), savedFAQ.getQuestion());

        return FAQResponse.from(savedFAQ);
    }

    // ==================== READ ====================

    /**
     * FAQ 상세 조회 (공개된 FAQ만)
     */
    @Transactional(readOnly = true)
    public FAQDetailResponse getFAQPublic(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        if (!faq.getIsPublic()) {
            throw new IllegalArgumentException("비공개 처리된 FAQ입니다");
        }

        return FAQDetailResponse.from(faq);
    }

    /**
     * FAQ 상세 조회 (관리자용 - 모든 FAQ)
     */
    @Transactional(readOnly = true)
    public FAQResponse getFAQAdmin(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        return FAQResponse.from(faq);
    }

    /**
     * 공개된 FAQ 전체 조회 - 페이징
     */
    @Transactional(readOnly = true)
    public Page<FAQListResponse> getFAQsPublic(Pageable pageable) {
        return faqRepository.findAllPublic(pageable).map(FAQListResponse::from);
    }

    /**
     * 관리자용 FAQ 전체 조회 - 페이징
     */
    @Transactional(readOnly = true)
    public Page<FAQResponse> getFAQsAdmin(Pageable pageable) {
        return faqRepository.findAllAdmin(pageable).map(FAQResponse::from);
    }

    /**
     * 공개 여부별 FAQ 조회
     */
    @Transactional(readOnly = true)
    public Page<FAQListResponse> getFAQsByPublicStatus(Boolean isPublic, Pageable pageable) {
        return faqRepository.findByIsPublic(isPublic, pageable).map(FAQListResponse::from);
    }

    /**
     * 잠금 여부별 FAQ 조회
     */
    @Transactional(readOnly = true)
    public Page<FAQListResponse> getFAQsByLockStatus(Boolean locked, Pageable pageable) {
        return faqRepository.findByLocked(locked, pageable).map(FAQListResponse::from);
    }

    /**
     * 공개 & 잠금 상태로 FAQ 조회
     */
    @Transactional(readOnly = true)
    public Page<FAQListResponse> getFAQsByPublicAndLock(Boolean isPublic, Boolean locked, Pageable pageable) {
        return faqRepository.findByIsPublicAndLocked(isPublic, locked, pageable).map(FAQListResponse::from);
    }

    /**
     * FAQ 검색 (공개된 FAQ만 - 질문만)
     */
    @Transactional(readOnly = true)
    public Page<FAQListResponse> searchFAQsPublic(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getFAQsPublic(pageable);
        }
        return faqRepository.searchPublic(keyword.trim(), pageable).map(FAQListResponse::from);
    }

    /**
     * FAQ 검색 (공개된 FAQ만 - 질문 + 답변)
     */
    @Transactional(readOnly = true)
    public Page<FAQDetailResponse> searchFAQsPublicFull(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 키워드가 없으면 기본 목록 조회 후 DetailResponse로 변환 (본문 제외 경량화 가능 시 고려)
            return getFAQsPublic(pageable).map(response ->
                    FAQDetailResponse.builder()
                            .id(response.getId())
                            .question(response.getQuestion())
                            // 목록 조회라 답변 내용이 없을 수 있음. 필요시 로직 분리 권장
                            .createdAt(response.getCreatedAt())
                            .build()
            );
        }
        return faqRepository.searchPublicFull(keyword.trim(), pageable).map(FAQDetailResponse::from);
    }

    /**
     * FAQ 검색 (관리자용 - 질문 + 답변)
     */
    @Transactional(readOnly = true)
    public Page<FAQResponse> searchFAQsAdmin(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getFAQsAdmin(pageable);
        }
        return faqRepository.searchAll(keyword.trim(), pageable).map(FAQResponse::from);
    }

    /**
     * 최근 추가된 FAQ 조회 (캐시용)
     */
    @Transactional(readOnly = true)
    public List<FAQListResponse> getRecentFAQs(int limit) {
        return faqRepository.findRecentPublic(limit).stream()
                .map(FAQListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 최근 업데이트된 FAQ 조회 (캐시용)
     */
    @Transactional(readOnly = true)
    public List<FAQDetailResponse> getRecentlyUpdatedFAQs(int limit) {
        return faqRepository.findRecentlyUpdatedPublic(limit).stream()
                .map(FAQDetailResponse::from)
                .collect(Collectors.toList());
    }

    // ==================== UPDATE ====================

    /**
     * FAQ 업데이트
     */
    public FAQResponse updateFAQ(Long faqId, FAQUpdateRequest request) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        // 질문 변경 시 중복 체크
        if (!faq.getQuestion().equals(request.getQuestion()) &&
                faqRepository.existsByQuestion(request.getQuestion())) {
            throw new IllegalArgumentException("이미 존재하는 질문입니다");
        }

        faq.update(
                request.getQuestion(),
                request.getAnswer(),
                request.getLocked() != null ? request.getLocked() : faq.getLocked(), // locked 값 유지 로직
                request.getIsPublic()
        );

        FAQ updatedFAQ = faqRepository.save(faq);
        log.info("FAQ updated: id={}, question={}", updatedFAQ.getId(), updatedFAQ.getQuestion());

        return FAQResponse.from(updatedFAQ);
    }

    /**
     * FAQ 공개 여부 토글
     */
    public FAQResponse togglePublic(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        faq.setPublic(!faq.getIsPublic());
        // save 호출은 Dirty Checking으로 대체 가능하지만 명시적으로 호출
        return FAQResponse.from(faq);
    }

    /**
     * FAQ 잠금 여부 토글
     */
    public FAQResponse toggleLock(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        faq.toggleLock();
        return FAQResponse.from(faq);
    }

    /**
     * FAQ 잠금
     */
    public FAQResponse lockFAQ(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        if (faq.getLocked()) {
            throw new IllegalArgumentException("이미 잠금 상태입니다");
        }

        faq.lock();
        return FAQResponse.from(faq);
    }

    /**
     * FAQ 잠금 해제
     */
    public FAQResponse unlockFAQ(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        if (!faq.getLocked()) {
            throw new IllegalArgumentException("잠금 상태가 아닙니다");
        }

        faq.unlock();
        return FAQResponse.from(faq);
    }

    /**
     * 여러 FAQ의 공개 여부 일괄 변경 (최적화)
     */
    public void bulkUpdatePublic(List<Long> faqIds, Boolean isPublic) {
        // [성능 최적화] stream().filter() 제거 -> findAllById로 한 번에 조회
        List<FAQ> faqs = faqRepository.findAllById(faqIds);

        if (faqs.size() != faqIds.size()) {
            throw new IllegalArgumentException("요청하신 ID 중 존재하지 않는 FAQ가 포함되어 있습니다.");
        }

        faqRepository.bulkUpdatePublic(faqIds, isPublic);
        log.info("Bulk updated {} FAQs public status to {}", faqIds.size(), isPublic);
    }

    /**
     * 여러 FAQ의 잠금 여부 일괄 변경 (최적화)
     */
    public void bulkUpdateLocked(List<Long> faqIds, Boolean locked) {
        // [성능 최적화] stream().filter() 제거 -> findAllById로 한 번에 조회
        List<FAQ> faqs = faqRepository.findAllById(faqIds);

        if (faqs.size() != faqIds.size()) {
            throw new IllegalArgumentException("요청하신 ID 중 존재하지 않는 FAQ가 포함되어 있습니다.");
        }

        faqRepository.bulkUpdateLocked(faqIds, locked);
        log.info("Bulk updated {} FAQs locked status to {}", faqIds.size(), locked);
    }

    // ==================== DELETE ====================

    /**
     * FAQ 삭제 (논리 삭제)
     */
    public void deleteFAQ(Long faqId) {
        FAQ faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다"));

        faqRepository.delete(faq);
        log.info("FAQ deleted: id={}", faqId);
    }

    /**
     * 여러 FAQ 삭제 (최적화)
     */
    public void bulkDeleteFAQs(List<Long> faqIds) {
        // [성능 최적화] N+1 문제 해결 (stream loop 제거)
        List<FAQ> faqs = faqRepository.findAllById(faqIds);

        if (faqs.size() != faqIds.size()) {
            throw new IllegalArgumentException("요청하신 ID 중 존재하지 않는 FAQ가 포함되어 있습니다.");
        }

        // JPA deleteAll -> 각 엔티티의 @SQLDelete 실행됨 (Soft Delete)
        faqRepository.deleteAll(faqs);

        log.info("Bulk deleted {} FAQs", faqIds.size());
    }

    // ==================== Utility Methods ====================

    /**
     * FAQ 통계
     */
    @Transactional(readOnly = true)
    public FAQStatistics getStatistics() {
        return FAQStatistics.builder()
                .totalCount(faqRepository.countAll())
                .publicCount(faqRepository.countPublic())
                .lockedCount(faqRepository.countLocked())
                .build();
    }

    /**
     * 공개된 FAQ 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasPublicFAQs() {
        return faqRepository.existsPublic();
    }

    // ==================== Inner Class ====================

    @lombok.Getter
    @lombok.Builder
    public static class FAQStatistics {
        private Long totalCount;
        private Long publicCount;
        private Long lockedCount;
    }
}