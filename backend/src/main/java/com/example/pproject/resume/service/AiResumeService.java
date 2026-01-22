package com.example.pproject.resume.service;

import com.example.pproject.resume.dto.AiResumeRequest;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient; // Spring Boot 3.2+ 권장

@Service
@RequiredArgsConstructor
@Slf4j
public class AiResumeService {

    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper; // JSON 변환용

    // Python AI 서버 주소 (application.yml에서 관리 권장)
    private final String AI_SERVER_URL = "http://localhost:8000/api/ai/resume";

    /**
     * AI 분석 요청 (비동기 처리 권장)
     * - 요구사항 명세: "Python API 호출은 메인 스레드를 차단하지 않는 비동기 방식(@Async)으로 처리"
     */
    @Async
    @Transactional(readOnly = true)
    public void requestAnalysis(Long resumeId, String summaryType) {
        // 1. 이력서 조회
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        // 2. DTO 변환 (우리가 만든 AiResumeRequest 사용!)
        AiResumeRequest aiRequestPayload = AiResumeRequest.from(resume, summaryType);

        try {
            // 3. Python 서버로 전송 (RestClient 사용 예시)
            RestClient restClient = RestClient.create();

            String response = restClient.post()
                    .uri(AI_SERVER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(aiRequestPayload)
                    .retrieve()
                    .body(String.class);

            log.info("AI Server Response: {}", response);

            // 4. (선택) 여기서 바로 DB 업데이트를 하거나,
            //    Python 서버가 분석 완료 후 별도 Webhook으로 결과를 보내주기를 기다림

        } catch (Exception e) {
            log.error("AI 서버 통신 중 오류 발생: {}", e.getMessage());
            // 실패 시 재시도 로직이나 상태 업데이트 필요
        }
    }
}