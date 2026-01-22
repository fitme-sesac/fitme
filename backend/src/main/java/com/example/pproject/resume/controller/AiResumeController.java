package com.example.pproject.resume.controller;

import com.example.pproject.global.response.ApiResponse;
import com.example.pproject.resume.service.AiResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiResumeController {

    private final AiResumeService aiResumeService;

    /**
     * 이력서 AI 첨삭 및 요약 요청
     * - 프론트엔드에서 "이력서 ID"와 "요약 타입"을 보내면,
     * - 백엔드가 이력서 데이터를 조회해서 AiResumeRequest로 변환 후 Python 서버로 전송
     */
    @PostMapping("/resumes/analyze")
    public ApiResponse<Void> analyzeResume(@RequestBody AiResumeRequestDTO request) {
        aiResumeService.requestAnalysis(request.getResumeId(), request.getSummaryType());

        return ApiResponse.success("AI 분석 요청이 성공적으로 접수되었습니다.", null);
    }

    @lombok.Data
    public static class AiResumeRequestDTO {
        private Long resumeId;
        private String summaryType;
    }
}