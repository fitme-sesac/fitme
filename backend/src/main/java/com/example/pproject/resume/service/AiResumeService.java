package com.example.pproject.resume.service;

import com.example.pproject.Constant.SummaryStatus;
import com.example.pproject.Config.AiServerConfig;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
import com.example.pproject.resume.dto.AiResumeRequest;
import com.example.pproject.resume.dto.AiResumeResponse;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiResumeService {

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final AiServerConfig aiServerConfig;

    @Async
    @Transactional
    public CompletableFuture<AiResumeResponse> requestAnalysis(Long resumeId, String summaryType, Long userId) {
        return requestAnalysisWithJob(resumeId, summaryType, null, userId);
    }

    @Async
    @Transactional
    public CompletableFuture<AiResumeResponse> requestAnalysisWithJob(
            Long resumeId, String summaryType, Long jobId, Long userId) {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        if (!resume.getUser().getId().equals(userId)) {
            throw new SecurityException("본인의 이력서만 AI 분석을 요청할 수 있습니다.");
        }

        if (resume.getSummaryStatus() == SummaryStatus.PROCESSING) {
            log.warn("이미 AI 분석이 진행 중입니다. resumeId: {}", resumeId);
            return CompletableFuture.completedFuture(
                    AiResumeResponse.builder()
                            .resumeId(resumeId)
                            .status("ALREADY_PROCESSING")
                            .errorMessage("이미 AI 분석이 진행 중입니다.")
                            .build()
            );
        }

        updateSummaryStatus(resume, SummaryStatus.PENDING);

        AiResumeRequest aiRequestPayload;

        if (jobId != null) {
            JobEntity job = jobRepository.findByIdAndNotDeleted(jobId).orElse(null);

            if (job != null) {
                // ✅ stack이 String[] 이므로 배열용 파서 사용
                List<String> requiredSkills = parseStack(job.getStack());

                aiRequestPayload = AiResumeRequest.from(
                        resume,
                        summaryType,
                        job.getId(),
                        job.getTitle(),
                        job.getDescription(),
                        requiredSkills,
                        job.getLocation(),
                        job.getSalaryText() != null ? formatSalary(job.getSalaryText()) : null,
                        null
                );
                log.info("채용공고 맞춤 AI 분석 요청 - resumeId: {}, jobId: {}", resumeId, jobId);
            } else {
                aiRequestPayload = AiResumeRequest.from(resume, summaryType);
                log.info("채용공고 없이 AI 분석 요청 (jobId 없음) - resumeId: {}", resumeId);
            }
        } else {
            aiRequestPayload = AiResumeRequest.from(resume, summaryType);
            log.info("일반 AI 분석 요청 - resumeId: {}", resumeId);
        }

        updateSummaryStatus(resume, SummaryStatus.PROCESSING);

        return callAiServerWithRetry(resume, aiRequestPayload);
    }

    private CompletableFuture<AiResumeResponse> callAiServerWithRetry(
            Resume resume, AiResumeRequest aiRequestPayload) {

        int maxAttempts = aiServerConfig.getRetry().getMaxAttempts();
        long delayMillis = aiServerConfig.getRetry().getDelayMillis();

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("AI 서버 호출 시도 {}/{} - resumeId: {}", attempt, maxAttempts, resume.getId());

                AiResumeResponse response = callAiServer(aiRequestPayload);

                if (response.isSuccess()) {
                    processSuccessResponse(resume, response);
                    log.info("AI 분석 성공 - resumeId: {}", resume.getId());
                    return CompletableFuture.completedFuture(response);
                } else {
                    log.warn("AI 서버 응답 실패 - resumeId: {}, error: {}", resume.getId(), response.getErrorMessage());
                    lastException = new RuntimeException(response.getErrorMessage());
                }

            } catch (Exception e) {
                lastException = e;
                log.warn("AI 서버 호출 실패 (시도 {}/{}) - resumeId: {}, error: {}",
                        attempt, maxAttempts, resume.getId(), e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMillis * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        updateSummaryStatus(resume, SummaryStatus.FAILED);
        log.error("AI 분석 최종 실패 - resumeId: {}", resume.getId(), lastException);

        return CompletableFuture.completedFuture(
                AiResumeResponse.builder()
                        .resumeId(resume.getId())
                        .status("FAILED")
                        .errorMessage("AI 서버 통신 실패: " +
                                (lastException != null ? lastException.getMessage() : "알 수 없는 오류"))
                        .build()
        );
    }

    private AiResumeResponse callAiServer(AiResumeRequest request) {
        RestClient restClient = RestClient.builder()
                .baseUrl(aiServerConfig.getBaseUrl())
                .build();

        return restClient.post()
                .uri(aiServerConfig.getResumeAnalyzePath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("AI 서버 오류: " + res.getStatusCode());
                })
                .body(AiResumeResponse.class);
    }

    @Transactional
    protected void processSuccessResponse(Resume resume, AiResumeResponse response) {
        resume.updateAiAnalysis(
                response.getSummary(),
                response.getSkillRecommendations()
        );
        resumeRepository.save(resume);
        log.info("AI 분석 결과 저장 완료 - resumeId: {}", resume.getId());
    }

    @Transactional
    protected void updateSummaryStatus(Resume resume, SummaryStatus status) {
        resume.updateSummaryStatus(status);
        resumeRepository.save(resume);
    }

    @Transactional(readOnly = true)
    public SummaryStatus getAnalysisStatus(Long resumeId, Long userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        if (!resume.getUser().getId().equals(userId)) {
            throw new SecurityException("본인의 이력서만 조회할 수 있습니다.");
        }
        return resume.getSummaryStatus();
    }

    // ✅ 신규: 배열(text[]) 파서
    private List<String> parseStack(String[] stack) {
        if (stack == null || stack.length == 0) return Collections.emptyList();

        return Arrays.stream(stack)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                // 혹시 저장된 값이 " {Java,Spring} " 같이 들어온 케이스 방어
                .map(s -> s.replaceAll("^\\{|\\}$", ""))
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    // (선택) 기존 String 파서도 남겨두기: 다른 곳에서 문자열로 들어오는 케이스 대비
    @SuppressWarnings("unused")
    private List<String> parseStack(String stack) {
        if (stack == null || stack.isBlank()) return Collections.emptyList();
        String cleaned = stack.replaceAll("^\\{|\\}$", "");
        return Arrays.stream(cleaned.split("[,/\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String formatSalary(Long salary) {
        if (salary == null) return null;
        return String.format("연봉 %,d만원", salary);
    }
}
