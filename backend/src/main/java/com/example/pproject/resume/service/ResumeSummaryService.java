package com.example.pproject.resume.service;

import com.example.pproject.Config.AiServerConfig;
import com.example.pproject.Constant.SummaryStatus;
import com.example.pproject.resume.dto.AiSummaryResponse;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 이력서 요약 + 임베딩 생성 서비스
 * 
 * 아키텍처:
 * 1. Java: DB 상태 관리 (PENDING → PROCESSING → COMPLETED/FAILED)
 * 2. Python: 순수 계산만 (요약 생성 + 임베딩 생성)
 * 3. Java: 결과 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeSummaryService {

    private final ResumeRepository resumeRepository;
    private final AiServerConfig aiServerConfig;

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 2000;

    /**
     * 이력서 요약 요청 (비동기)
     * 
     * 동시성 제어 및 트랜잭션 전략:
     * 1. startProcessing: REQUIRES_NEW로 별도 트랜잭션 실행 -> 상태를 PROCESSING으로 즉시 변경 및 커밋
     * 2. AI 호출: 트랜잭션 없이 수행 (DB 커넥션 점유 방지)
     * 3. 결과 저장: 성공 시 COMPLETED, 실패 시 FAILED로 별도 트랜잭션 업데이트
     */
    @Async("aiTaskExecutor")
    public CompletableFuture<AiSummaryResponse> requestSummary(Long resumeId, Long userId) {
        try {
            // 1. [별도 트랜잭션] 상태 진입 시도 (동시성 제어)
            // 이미 처리 중이면 예외 발생으로 종료됨
            Resume resume = tryStartProcessing(resumeId, userId);

            // 2. [트랜잭션 없음] AI 호출 (오래 걸림)
            return callAiWorkerWithRetry(resume);

        } catch (IllegalStateException e) {
            // 이미 처리 중인 경우 (정상 방어)
            log.warn("요약 요청 거부 (이미 처리 중): resumeId={}", resumeId);
            return CompletableFuture.completedFuture(
                    AiSummaryResponse.builder().summary("이미 처리 중입니다.").build());
        } catch (Exception e) {
            // 그 외 치명적 오류 발생 시 FAILED 처리
            log.error("요약 요청 처리 중 시스템 오류: resumeId={}", resumeId, e);
            updateStatusById(resumeId, SummaryStatus.FAILED);
            return CompletableFuture.completedFuture(
                    AiSummaryResponse.builder().summary("시스템 오류 발생").build());
        }
    }

    /**
     * 상태 진입 시도 (Atomic Check & Update)
     * 트랜잭션을 분리(REQUIRES_NEW)하여 즉시 커밋함 -> 다른 스레드가 바로 알 수 있게 함
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Resume tryStartProcessing(Long resumeId, Long userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다. ID: " + resumeId));

        if (!resume.getUser().getId().equals(userId)) {
            throw new SecurityException("본인의 이력서만 요약을 요청할 수 있습니다.");
        }

        if (resume.getSummaryStatus() == SummaryStatus.PROCESSING) {
            throw new IllegalStateException("ALREADY_PROCESSING");
        }

        // PENDING 상태는 건너뛰고 바로 PROCESSING으로 마킹 (단순화)
        resume.updateSummaryStatus(SummaryStatus.PROCESSING);
        Resume saved = resumeRepository.save(resume);

        // [중요] 연관 데이터 강제 로딩 (LazyInitializationException 방지)
        // 트랜잭션이 끝나기 전에 필요한 데이터를 로딩해둡니다.
        if (saved.getProjects() != null)
            saved.getProjects().size();
        if (saved.getCareers() != null)
            saved.getCareers().size();

        return saved; // Detached 되지만 데이터는 로딩됨
    }

    /**
     * 실패 상태 강제 업데이트 (ID 기반)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateStatusById(Long resumeId, SummaryStatus status) {
        resumeRepository.findById(resumeId).ifPresent(resume -> {
            resume.updateSummaryStatus(status);
            resumeRepository.save(resume);
        });
    }

    /**
     * AI Worker 호출 (재시도 로직 포함)
     */
    private CompletableFuture<AiSummaryResponse> callAiWorkerWithRetry(Resume resume) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                log.info("[AI Worker] 호출 시도 {}/{} - resumeId: {}", attempt, MAX_RETRY, resume.getId());

                // Python 요청 생성 (Resume 객체는 이미 로딩 완료됨)
                Map<String, Object> request = buildPythonRequest(resume);

                // RestClient로 호출
                AiSummaryResponse response = callPythonApi(request);

                if (response != null && response.isSuccess()) {
                    // 성공: DB에 결과 저장 (새 트랜잭션)
                    saveResults(resume.getId(), response);
                    log.info("[AI Worker] 요약 성공 - resumeId: {}", resume.getId());
                    return CompletableFuture.completedFuture(response);
                } else {
                    log.warn("[AI Worker] 응답 실패 - resumeId: {}", resume.getId());
                    lastException = new RuntimeException("AI 응답이 비어있음");
                }

            } catch (Exception e) {
                lastException = e;
                log.warn("[AI Worker] 호출 실패 (시도 {}/{}) - resumeId: {}, error: {}",
                        attempt, MAX_RETRY, resume.getId(), e.getMessage());

                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 모든 재시도 실패
        updateStatusById(resume.getId(), SummaryStatus.FAILED);
        log.error("[AI Worker] 최종 실패 - resumeId: {}", resume.getId(), lastException);

        return CompletableFuture.completedFuture(
                AiSummaryResponse.builder()
                        .summary("요약 생성 실패: " + (lastException != null ? lastException.getMessage() : "알 수 없는 오류"))
                        .build());
    }

    /**
     * Python API 호출 (RestClient)
     */
    private AiSummaryResponse callPythonApi(Map<String, Object> request) {
        RestClient restClient = RestClient.builder()
                .baseUrl(aiServerConfig.getBaseUrl())
                .build();

        return restClient.post()
                .uri(aiServerConfig.getResumeSummaryPath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("AI Worker 오류: " + res.getStatusCode());
                })
                .body(AiSummaryResponse.class);
    }

    /**
     * Python 요청 생성 (ResumeRequest 형식)
     */
    private Map<String, Object> buildPythonRequest(Resume resume) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM");

        // basic_info
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("title", resume.getTitle());
        basicInfo.put("re_stack", resume.getReStack() != null ? resume.getReStack() : Collections.emptyList());
        basicInfo.put("field", resume.getField() != null ? resume.getField().name() : "RESUME");

        // education (간단히)
        Map<String, Object> education = new HashMap<>();
        education.put("status", resume.getSchoolState() != null ? resume.getSchoolState() : "졸업");
        education.put("major", resume.getSchoolClass() != null ? resume.getSchoolClass() : "비전공");
        education.put("school_name", resume.getSchool());
        basicInfo.put("education", education);

        // preference
        Map<String, Object> preference = new HashMap<>();
        preference.put("location", resume.getPreferenceLocation() != null ? resume.getPreferenceLocation() : "");
        preference.put("salary", resume.getPreferenceSalary() != null ? resume.getPreferenceSalary() : "");
        preference.put("employment_type", resume.getEmploymentType() != null ? resume.getEmploymentType() : "");
        basicInfo.put("preference", preference);

        // projects
        List<Map<String, Object>> projects = resume.getProjects().stream()
                .map(p -> {
                    Map<String, Object> proj = new HashMap<>();
                    proj.put("project_name", p.getTitle());
                    proj.put("start_date", formatDate(p.getStartDate(), formatter));
                    proj.put("end_date", formatDate(p.getEndDate(), formatter));
                    proj.put("total_tech_stack", parseTechStack(p.getTechStack()));
                    proj.put("contribution", "");
                    proj.put("description", p.getDescription());
                    return proj;
                })
                .collect(Collectors.toList());

        // careers
        List<Map<String, Object>> careers = resume.getCareers().stream()
                .map(c -> {
                    Map<String, Object> career = new HashMap<>();
                    career.put("company_name", c.getCompanyName());
                    career.put("role", c.getRole());
                    career.put("start_date", formatDate(c.getStartDate(), formatter));
                    career.put("end_date", formatDate(c.getEndDate(), formatter));
                    career.put("description", "");
                    return career;
                })
                .collect(Collectors.toList());

        // 최종 요청
        Map<String, Object> request = new HashMap<>();
        request.put("resume_id", resume.getId());
        request.put("basic_info", basicInfo);
        request.put("content", resume.getContent() != null ? resume.getContent() : "");
        request.put("projects", projects);
        request.put("careers", careers);
        request.put("file_links", Collections.emptyList());
        request.put("summary_type", "STRUCTURED");
        request.put("include_reasoning", false);

        return request;
    }

    /**
     * 결과 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveResults(Long resumeId, AiSummaryResponse response) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        // 요약 및 임베딩 저장
        resume.updateAiAnalysisWithEmbedding(response.getSummary(), response.getEmbedding());

        // 상태: COMPLETED
        resume.updateSummaryStatus(SummaryStatus.COMPLETED);
        resumeRepository.save(resume);

        log.info("[DB 저장] 요약 완료 - resumeId: {}, evalInfo: {}",
                resume.getId(), response.getEvalInfo());
    }

    /**
     * 요약 상태 조회
     */
    @Transactional(readOnly = true)
    public SummaryStatus getStatus(Long resumeId, Long userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        if (!resume.getUser().getId().equals(userId)) {
            throw new SecurityException("본인의 이력서만 조회할 수 있습니다.");
        }

        return resume.getSummaryStatus();
    }

    // ========== Helper Methods ==========

    private String formatDate(LocalDate date, DateTimeFormatter formatter) {
        return date != null ? date.format(formatter) : "";
    }

    private List<String> parseTechStack(String techStack) {
        if (techStack == null || techStack.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(techStack.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
