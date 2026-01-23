package com.example.pproject.faq.service;

import com.example.pproject.faq.dto.FAQCreateRequest;
import com.example.pproject.faq.dto.FAQResponse;
import com.example.pproject.faq.entity.FAQ;
import com.example.pproject.faq.repository.FAQRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FAQServiceTest {

    @Mock
    private FAQRepository faqRepository;

    @InjectMocks
    private FAQService faqService;

    @Test
    @DisplayName("중복된 질문으로 FAQ를 만들면 실패해야 한다")
    void createFAQ_Duplicate() {
        // 1. [Given]
        FAQCreateRequest request = FAQCreateRequest.builder()
                .question("같은 질문")
                .answer("답변")
                .isPublic(true)
                .build();

        // "이미 존재하는 질문이니?" 라고 물으면 "응(true)"이라고 대답해라
        given(faqRepository.existsByQuestion("같은 질문")).willReturn(true);

        // 2. [When & Then] 에러가 나야 정상
        assertThatThrownBy(() -> faqService.createFAQ(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 질문입니다");
    }

    @Test
    @DisplayName("FAQ 일괄 삭제가 정상 작동한다")
    void bulkDeleteFAQs() {
        // 1. [Given] 삭제할 ID 목록
        List<Long> ids = List.of(1L, 2L);

        // 가짜 FAQ 객체들
        FAQ faq1 = FAQ.builder().id(1L).build();
        FAQ faq2 = FAQ.builder().id(2L).build();
        List<FAQ> mockFaqs = List.of(faq1, faq2);

        // "1,2번 찾아줘" 하면 "여기 있어" 하고 리턴
        given(faqRepository.findAllById(ids)).willReturn(mockFaqs);

        // 2. [When] 삭제 실행
        faqService.bulkDeleteFAQs(ids);

        // 3. [Then] 실제로 deleteAll이 호출되었는지 확인
        verify(faqRepository).deleteAll(mockFaqs);
    }
}