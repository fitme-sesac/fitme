package com.example.pproject.faq.controller;

import com.example.pproject.faq.dto.FaqCreateRequest;
import com.example.pproject.faq.dto.FaqResponse;
import com.example.pproject.faq.service.FaqService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FaqController.class)
class FaqControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FaqService faqService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("FAQ 등록 성공: 관리자 권한(ADMIN)이 있을 때")
    @WithMockUser(roles = "ADMIN") // 관리자로 로그인 가정
    void createFaq_Success_Admin() throws Exception {
        // given
        FaqCreateRequest request = FaqCreateRequest.builder()
                .question("질문")
                .answer("답변")
                .isPublic(true)
                .locked(true)
                .build();

        FaqResponse response = FaqResponse.builder()
                .id(1L)
                .question("질문")
                .build();

        given(faqService.createFaq(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/faqs")
                        .with(csrf()) // POST 요청 시 CSRF 토큰 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("FAQ 등록 실패: 일반 사용자(USER)가 시도할 때 403 Forbidden")
    @WithMockUser(roles = "USER") // 일반 사용자로 로그인 가정
    void createFaq_Fail_User() throws Exception {
        // given
        FaqCreateRequest request = FaqCreateRequest.builder()
                .question("해킹 시도")
                .answer("...")
                .isPublic(true)
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/faqs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden()); // 403 에러 예상
    }

    @Test
    @DisplayName("FAQ 목록 조회: 인증 없이도 공개 API는 접근 가능")
    void getPublicFaqList_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/faqs/public/list"))
                .andDo(print())
                .andExpect(status().isOk()); // 200 OK 예상
    }
}