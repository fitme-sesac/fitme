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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiResumeService {

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final AiServerConfig aiServerConfig;

    /**
     * AI 이력서 분석 요청 (기본 - 채용공고 없이)
     */
    @Async
    @Transactional
    public CompletableFuture<AiResumeResponse> requestAnalysis(Long resumeId, String summaryType, Long userId) {
        return requestAnalysisWithJob(resumeId, summaryType, null, userId);
    }

    /**
     * AI 이력서 분석 요청 (채용공고 맞춤 첨삭)
     * - 특정 채용공고에 맞춰 이력서 첨삭 제안
     */
    @Async
    @Transactional
    public CompletableFuture<AiResumeResponse> requestAnalysisWithJob(
            Long resumeId, String summaryType, Long jobId, Long userId) {

        // 1. 이력서 조회 및 권한 검증
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        // 소유자 검증
        if (!resume.getUser().getId().equals(userId)) {
            throw new SecurityException("본인의 이력서만 AI 분석을 요청할 수 있습니다.");
        }

        // 이미 처리 중인 경우 중복 요청 방지
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

        // 2. 상태를 PENDING → PROCESSING으로 변경
        updateSummaryStatus(resume, SummaryStatus.PENDING);

        // 3. 채용공고 정보 조회 (있는 경우)
        AiResumeRequest aiRequestPayload;
        if (jobId != null) {
            JobEntity job = jobRepository.findByIdAndNotDeleted(jobId)
                    .orElse(null);
            
            if (job != null) {
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
                        null // companyName은 employer에서 가져와야 함
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

        // 4. 상태를 PROCESSING으로 변경
        updateSummaryStatus(resume, SummaryStatus.PROCESSING);

        // 5. AI 서버 호출 (재시도 로직 포함)
        return callAiServerWithRetry(resume, aiRequestPayload);
    }

    /**
     * AI 서버 호출 (재시도 로직 포함)
     */
    private CompletableFuture<AiResumeResponse> callAiServerWithRetry(
            Resume resume, AiResumeRequest aiRequestPayload) {
        
        int maxAttempts = aiServerConfig.getRetry().getMaxAttempts();
        long delayMillis = aiServerConfig.getRetry().getDelayMillis();

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("AI 서버 호출 시도 {}/{} - resumeId: {}", 
                        attempt, maxAttempts, resume.getId());

                AiResumeResponse response = callAiServer(aiRequestPayload);

                if (response.isSuccess()) {
                    // 성공 시 결과 저장
                    processSuccessResponse(resume, response);
                    log.info("AI 분석 성공 - resumeId: {}", resume.getId());
                    return CompletableFuture.completedFuture(response);
                } else {
                    log.warn("AI 서버 응답 실패 - resumeId: {}, error: {}", 
                            resume.getId(), response.getErrorMessage());
                    lastException = new RuntimeException(response.getErrorMessage());
                }

            } catch (Exception e) {
                lastException = e;
                log.warn("AI 서버 호출 실패 (시도 {}/{}) - resumeId: {}, error: {}", 
                        attempt, maxAttempts, resume.getId(), e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMillis * attempt); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 모든 재시도 실패
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

    /**
     * AI 서버 호출
     */
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

    /**
     * 성공 응답 처리 - DB 업데이트
     */
    @Transactional
    protected void processSuccessResponse(Resume resume, AiResumeResponse response) {
        // AI 분석 결과 업데이트
        resume.updateAiAnalysis(
                response.getSummary(),
                response.getSkillRecommendations()
        );

        resumeRepository.save(resume);
        log.info("AI 분석 결과 저장 완료 - resumeId: {}", resume.getId());
    }

    /**
     * 상태 업데이트
     */
    @Transactional
    protected void updateSummaryStatus(Resume resume, SummaryStatus status) {
        resume.updateSummaryStatus(status);
        resumeRepository.save(resume);
    }

    /**
     * AI 분석 상태 조회
     */
    @Transactional(readOnly = true)
    public SummaryStatus getAnalysisStatus(Long resumeId, Long userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        // 소유자 검증
        if (!resume.getUser().getId().equals(userId)) {
            throw new SecurityException("본인의 이력서만 조회할 수 있습니다.");
        }

        return resume.getSummaryStatus();
    }

    /**
     * 스택 문자열 파싱 (콤마/슬래시/공백으로 분리)
     */
    private List<String> parseStack(String stack) {
        if (stack == null || stack.isBlank()) {
            return Collections.emptyList();
        }
        // {} 제거 및 파싱
        String cleaned = stack.replaceAll("[{}]", "");
        return Arrays.stream(cleaned.split("[,/\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 급여 포맷팅
     */
    private String formatSalary(Long salary) {
        if (salary == null) return null;
        return String.format("연봉 %,d만원", salary);
    }
}
