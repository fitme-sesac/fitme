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
     * 수정됨: isPublic 파라미터(true/false)에 따라 정확히 필터링하도록 로직 개선
     */
    @Transactional(readOnly = true)
    public Page<FaqListResponse> getFaqList(Boolean isPublic, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Faq> faqs;

        // 1. 키워드 검색이 있는 경우
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (isPublic != null) {
                // [Fix] 기존에는 false여도 searchPublicByKeyword를 호출하던 버그 수정
                // isPublic 값(true/false)을 그대로 전달하여 조회
                faqs = faqRepository.searchByKeywordAndIsPublic(keyword, isPublic, pageable);
            } else {
                // 공개 여부 상관없이 전체 검색
                faqs = faqRepository.searchByKeyword(keyword, pageable);
            }
        }
        // 2. 키워드 검색이 없는 경우 (기본 목록 조회)
        else {
            if (isPublic != null) {
                // 공개/비공개 필터링
                faqs = faqRepository.findByIsPublicAndNotDeleted(isPublic, pageable);
            } else {
                // 전체 조회
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

        // Entity에서 @Builder.Default로 locked=true 설정을 권장하지만,
        // 만약 DTO에서 null이 넘어올 경우를 대비해 한 번 더 체크 가능 (선택 사항)
        // if (faq.getLocked() == null) faq.setLocked(true);

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

        // 질문이 변경되었는데, 변경하려는 질문이 이미 다른 FAQ에 존재하는지 검증
        if (!faq.getQuestion().equals(request.getQuestion()) &&
                faqRepository.existsByQuestionAndNotDeleted(request.getQuestion())) {
            log.warn("중복된 FAQ 질문으로 수정 시도: {}", request.getQuestion());
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 질문입니다");
        }

        // Dirty Checking
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setIsPublic(request.getIsPublic());

        if (request.getLocked() != null) {
            faq.setLocked(request.getLocked());
        }

        // JPA 변경 감지에 의해 트랜잭션 종료 시 업데이트되지만, 명시적 리턴을 위해 save 호출 또는 그냥 리턴
        // Auditing(@UpdateTimestamp) 동작을 보장하기 위해 saveAndFlush 혹은 그냥 둠 (Transaction 안이라 자동 반영)

        log.info("FAQ 수정 완료 - ID: {}", faq.getId());

        return FaqResponse.fromEntity(faq);
    }

    /**
     * ADM-FAQ-004: FAQ 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteFaq(Long id) {
        Faq faq = faqRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 FAQ 삭제 시도 - ID: {}", id);
                    return new BusinessException(ErrorCode.NOT_FOUND, "FAQ를 찾을 수 없습니다");
                });

        faq.delete(); // deletedAt 업데이트
        // faqRepository.save(faq); // Transactional 어노테이션이 있어 save 호출 안 해도 됨 (Dirty Checking)

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