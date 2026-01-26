// src/main/java/com/example/pproject/job/service/JobService.java
package com.example.pproject.job.service;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.dto.*;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
import com.example.pproject.outbox.producer.OutboxEventProducer;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.resume.service.ResumeSkillService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final UserRepository userRepository;
    private final OutboxEventProducer outboxEventProducer;
    private final ResumeRepository resumeRepository;
    private final ResumeSkillService resumeSkillService;

    /**
     * 한글-영어 기술스택 매핑 (양방향)
     * 한글로 검색해도 영어 스택을 찾을 수 있도록 지원
     */
    private static final Map<String, String> KOREAN_TO_ENGLISH_STACK = Map.ofEntries(
            // 프로그래밍 언어
            Map.entry("자바", "Java"),
            Map.entry("파이썬", "Python"),
            Map.entry("자바스크립트", "JavaScript"),
            Map.entry("타입스크립트", "TypeScript"),
            Map.entry("코틀린", "Kotlin"),
            Map.entry("스위프트", "Swift"),
            Map.entry("고", "Go"),
            Map.entry("고랭", "Go"),
            Map.entry("러스트", "Rust"),
            Map.entry("씨", "C"),
            Map.entry("씨플플", "C++"),
            Map.entry("씨샵", "C#"),
            Map.entry("루비", "Ruby"),
            Map.entry("펄", "Perl"),
            Map.entry("스칼라", "Scala"),
            Map.entry("다트", "Dart"),
            Map.entry("알", "R"),
            Map.entry("매트랩", "MATLAB"),
            Map.entry("피에이치피", "PHP"),

            // 프레임워크/라이브러리
            Map.entry("리액트", "React"),
            Map.entry("뷰", "Vue"),
            Map.entry("앵귤러", "Angular"),
            Map.entry("스프링", "Spring"),
            Map.entry("스프링부트", "Spring Boot"),
            Map.entry("장고", "Django"),
            Map.entry("플라스크", "Flask"),
            Map.entry("노드", "Node.js"),
            Map.entry("노드제이에스", "Node.js"),
            Map.entry("익스프레스", "Express"),
            Map.entry("넥스트", "Next.js"),
            Map.entry("넥스트제이에스", "Next.js"),
            Map.entry("넉스트", "Nuxt"),
            Map.entry("플러터", "Flutter"),
            Map.entry("리액트네이티브", "React Native"),
            Map.entry("레일즈", "Rails"),
            Map.entry("라라벨", "Laravel"),
            Map.entry("부트스트랩", "Bootstrap"),
            Map.entry("테일윈드", "Tailwind"),
            Map.entry("제이쿼리", "jQuery"),

            // 데이터베이스
            Map.entry("마이에스큐엘", "MySQL"),
            Map.entry("포스트그레스", "PostgreSQL"),
            Map.entry("포스트그레스큐엘", "PostgreSQL"),
            Map.entry("몽고디비", "MongoDB"),
            Map.entry("몽고", "MongoDB"),
            Map.entry("레디스", "Redis"),
            Map.entry("오라클", "Oracle"),
            Map.entry("엘라스틱서치", "Elasticsearch"),
            Map.entry("카산드라", "Cassandra"),
            Map.entry("다이나모디비", "DynamoDB"),

            // 클라우드/인프라
            Map.entry("에이더블유에스", "AWS"),
            Map.entry("아마존", "AWS"),
            Map.entry("애저", "Azure"),
            Map.entry("지씨피", "GCP"),
            Map.entry("구글클라우드", "GCP"),
            Map.entry("도커", "Docker"),
            Map.entry("쿠버네티스", "Kubernetes"),
            Map.entry("쿠베", "Kubernetes"),
            Map.entry("케이에이트에스", "Kubernetes"),
            Map.entry("젠킨스", "Jenkins"),
            Map.entry("테라폼", "Terraform"),
            Map.entry("앤서블", "Ansible"),
            Map.entry("엔진엑스", "Nginx"),
            Map.entry("아파치", "Apache"),
            Map.entry("리눅스", "Linux"),

            // 기타
            Map.entry("깃", "Git"),
            Map.entry("깃허브", "GitHub"),
            Map.entry("깃랩", "GitLab"),
            Map.entry("지라", "Jira"),
            Map.entry("슬랙", "Slack"),
            Map.entry("피그마", "Figma"),
            Map.entry("그래프큐엘", "GraphQL"),
            Map.entry("레스트", "REST"),
            Map.entry("에이피아이", "API"),
            Map.entry("마이크로서비스", "Microservices"),
            Map.entry("머신러닝", "Machine Learning"),
            Map.entry("딥러닝", "Deep Learning"),
            Map.entry("인공지능", "AI"),
            Map.entry("에이아이", "AI"),
            Map.entry("빅데이터", "Big Data"),
            Map.entry("데이터분석", "Data Analysis"),
            Map.entry("블록체인", "Blockchain")
    );

    // ====== 공통 헬퍼 ======

    private String[] cleanStackArray(String[] raw) {
        if (raw == null) return null;
        return Arrays.stream(raw)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    /**
     * 기업의 채용공고 목록 조회
     */
    public JobListResponseDTO getJobsByEmployer(String userid, int page, int size) {
        EmployerEntity employer = getEmployerByUserid(userid);

        Page<JobEntity> jobPage = jobRepository.findByEmployerIdAndNotDeleted(
                employer.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<JobDTO> jobs = jobPage.getContent().stream()
                .map(job -> toJobDTO(job, employer))
                .collect(Collectors.toList());

        return JobListResponseDTO.builder()
                .jobs(jobs)
                .page(page)
                .size(size)
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .build();
    }

    /**
     * 채용공고 상세 조회 (jobId 사용)
     */
    public JobDTO getJob(String userid, Long jobId) {
        EmployerEntity employer = getEmployerByUserid(userid);

        JobEntity job = jobRepository.findByIdAndNotDeleted(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }

        return toJobDTO(job, employer);
    }

    /**
     * 채용공고 상세 조회 (문자열 ID - 기존 API 호환)
     */
    public JobDTO getJob(String userid, String jobIdStr) {
        try {
            Long jobId = Long.parseLong(jobIdStr);
            return getJob(userid, jobId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("잘못된 채용공고 ID입니다.");
        }
    }

    /**
     * 채용공고 등록
     */
    @Transactional
    public JobDTO createJob(String userid, JobCreateDTO dto) {
        EmployerEntity employer = getEmployerByUserid(userid);
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (!"ACTIVE".equals(employer.getStatus())) {
            throw new IllegalStateException("기업이 활성 상태가 아닙니다.");
        }

        String summary = generateSummary(dto.getDescription());

        JobEntity job = JobEntity.builder()
                .employerId(employer.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .summary(summary)
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .location(dto.getLocation())
                .salaryText(dto.getSalaryText())
                .stack(cleanStackArray(dto.getStack())) // ✅ String[] 저장
                .requiredQuestions(dto.getRequiredQuestions())
                .build();

        jobRepository.save(job);
        log.info("채용공고 생성: {} (기업: {}, 요약: {})", job.getTitle(), employer.getName(), summary);

        try {
            outboxEventProducer.publishJobPostingCreatedEvent(
                    job.getId(),
                    user.getId().longValue(),
                    job.getTitle(),
                    employer.getName(),
                    job.getStatus()
            );
            log.info("채용공고 등록 알림 발행: jobId={}, userId={}", job.getId(), user.getId());
        } catch (Exception e) {
            log.error("채용공고 등록 알림 발행 실패: {}", e.getMessage());
        }

        return toJobDTO(job, employer);
    }

    /**
     * 채용공고 수정
     */
    @Transactional
    public JobDTO updateJob(String userid, Long jobId, JobUpdateDTO dto) {
        EmployerEntity employer = getEmployerByUserid(userid);
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        JobEntity job = jobRepository.findByIdAndNotDeleted(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        String oldStatus = job.getStatus();

        if (dto.getTitle() != null) job.setTitle(dto.getTitle());
        if (dto.getDescription() != null) {
            job.setDescription(dto.getDescription());
            job.setSummary(generateSummary(dto.getDescription()));
        }
        if (dto.getStatus() != null) job.setStatus(dto.getStatus());
        if (dto.getLocation() != null) job.setLocation(dto.getLocation());
        if (dto.getSalaryText() != null) job.setSalaryText(dto.getSalaryText());
        if (dto.getStack() != null) job.setStack(cleanStackArray(dto.getStack())); // ✅
        if (dto.getRequiredQuestions() != null) job.setRequiredQuestions(dto.getRequiredQuestions());

        jobRepository.save(job);
        log.info("채용공고 수정: {}", job.getTitle());

        try {
            if (dto.getStatus() != null && !dto.getStatus().equals(oldStatus)) {
                outboxEventProducer.publishJobPostingStatusChangedEvent(
                        job.getId(),
                        user.getId().longValue(),
                        job.getTitle(),
                        employer.getName(),
                        oldStatus,
                        dto.getStatus()
                );
            } else {
                outboxEventProducer.publishJobPostingUpdatedEvent(
                        job.getId(),
                        user.getId().longValue(),
                        job.getTitle(),
                        employer.getName()
                );
            }
            log.info("채용공고 수정 알림 발행: jobId={}, userId={}", job.getId(), user.getId());
        } catch (Exception e) {
            log.error("채용공고 수정 알림 발행 실패: {}", e.getMessage());
        }

        return toJobDTO(job, employer);
    }

    /**
     * 채용공고 수정 (문자열 ID - 기존 API 호환)
     */
    @Transactional
    public JobDTO updateJob(String userid, String jobIdStr, JobUpdateDTO dto) {
        try {
            Long jobId = Long.parseLong(jobIdStr);
            return updateJob(userid, jobId, dto);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("잘못된 채용공고 ID입니다.");
        }
    }

    /**
     * 채용공고 삭제 (soft delete)
     */
    @Transactional
    public void deleteJob(String userid, Long jobId) {
        EmployerEntity employer = getEmployerByUserid(userid);
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        JobEntity job = jobRepository.findByIdAndNotDeleted(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        String jobTitle = job.getTitle();

        job.setDeletedAt(Instant.now());
        job.setStatus("CLOSED");
        jobRepository.save(job);
        log.info("채용공고 삭제: {}", jobTitle);

        try {
            outboxEventProducer.publishJobPostingDeletedEvent(
                    job.getId(),
                    user.getId().longValue(),
                    jobTitle,
                    employer.getName()
            );
            log.info("채용공고 삭제 알림 발행: jobId={}, userId={}", job.getId(), user.getId());
        } catch (Exception e) {
            log.error("채용공고 삭제 알림 발행 실패: {}", e.getMessage());
        }
    }

    /**
     * 채용공고 삭제 (문자열 ID - 기존 API 호환)
     */
    @Transactional
    public void deleteJob(String userid, String jobIdStr) {
        try {
            Long jobId = Long.parseLong(jobIdStr);
            deleteJob(userid, jobId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("잘못된 채용공고 ID입니다.");
        }
    }

    // ===== 공개 채용공고 조회 (인증 없이 접근 가능) =====

    /**
     * 채용공고 필터 옵션 조회 (사용 가능한 스택/지역 목록)
     */
    public JobFilterOptionsDTO getFilterOptions() {
        List<String> stacks = jobRepository.findDistinctStacks();
        List<String> locations = jobRepository.findDistinctLocations();

        stacks = stacks.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        locations = locations.stream()
                .filter(l -> l != null && !l.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        log.info("필터 옵션 조회 - 스택: {}개, 지역: {}개", stacks.size(), locations.size());

        return JobFilterOptionsDTO.builder()
                .stacks(stacks)
                .locations(locations)
                .build();
    }

    /**
     * 공개 채용공고 목록 조회 (status='OPEN')
     */
    public JobListResponseDTO getPublicJobs(int page, int size, String keyword, String stack, String location) {
        Page<JobEntity> jobPage;
        PageRequest pageRequestWithSort = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageRequest pageRequestNoSort = PageRequest.of(page, size);

        if (keyword != null && !keyword.isBlank()) {
            String searchKeyword = convertKoreanToEnglish(keyword.trim());
            log.info("검색 키워드 변환: '{}' → '{}'", keyword.trim(), searchKeyword);
            jobPage = jobRepository.searchPublicJobs(searchKeyword, pageRequestNoSort);
        } else if (stack != null && !stack.isBlank()) {
            String searchStack = convertKoreanToEnglish(stack.trim());
            log.info("스택 필터 변환: '{}' → '{}'", stack.trim(), searchStack);
            jobPage = jobRepository.findPublicJobsByStack(searchStack, pageRequestNoSort);
        } else if (location != null && !location.isBlank()) {
            jobPage = jobRepository.findPublicJobsByLocation(location.trim(), pageRequestWithSort);
        } else {
            jobPage = jobRepository.findPublicJobs(pageRequestWithSort);
        }

        List<JobDTO> jobs = jobPage.getContent().stream()
                .map(this::toPublicJobDTO)
                .collect(Collectors.toList());

        return JobListResponseDTO.builder()
                .jobs(jobs)
                .page(page)
                .size(size)
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .build();
    }

    private String convertKoreanToEnglish(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return keyword;
        }

        String lowerKeyword = keyword.toLowerCase().trim();

        for (Map.Entry<String, String> entry : KOREAN_TO_ENGLISH_STACK.entrySet()) {
            if (entry.getKey().equals(lowerKeyword)) {
                return entry.getValue();
            }
        }

        for (Map.Entry<String, String> entry : KOREAN_TO_ENGLISH_STACK.entrySet()) {
            if (lowerKeyword.contains(entry.getKey())) {
                return keyword.replace(entry.getKey(), entry.getValue());
            }
        }

        return keyword;
    }

    /**
     * 공개 채용공고 상세 조회 (조회수 증가)
     */
    @Transactional
    public JobDTO getPublicJob(Long jobId) {
        JobEntity job = jobRepository.findPublicJobById(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        jobRepository.incrementViewCount(jobId);

        JobDTO dto = toPublicJobDTO(job);
        dto.setViewCount(job.getViewCount() + 1);

        return dto;
    }

    private JobDTO toPublicJobDTO(JobEntity job) {
        EmployerEntity employer = employerRepository.findById(job.getEmployerId()).orElse(null);

        return JobDTO.builder()
                .jobId(job.getId())
                .jobUid(String.valueOf(job.getId()))
                .title(job.getTitle())
                .description(job.getDescription())
                .summary(job.getSummary())
                .status(job.getStatus())
                .location(job.getLocation())
                .salaryText(job.getSalaryText())
                .salaryDisplay(formatSalary(job.getSalaryText()))
                // ✅ String[] 그대로 내려줌
                .stack(cleanStackArray(job.getStack()))
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .updatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null)
                .companyName(employer != null ? employer.getName() : "알 수 없음")
                .companyLogoUrl(employer != null ? employer.getLogoUrl() : null)
                .build();
    }

    // === Helper Methods ===

    private String generateSummary(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String cleaned = description.trim();

        String[] paragraphs = cleaned.split("\\n\\n|\\r\\n\\r\\n");
        String firstParagraph = paragraphs[0].trim();

        String[] sentences = firstParagraph.split("(?<=[.!?])\\s+");

        StringBuilder summary = new StringBuilder();
        int sentenceCount = 0;

        for (String sentence : sentences) {
            if (sentenceCount >= 2 || summary.length() + sentence.length() > 200) {
                break;
            }
            if (summary.length() > 0) {
                summary.append(" ");
            }
            summary.append(sentence.trim());
            sentenceCount++;
        }

        String result = summary.toString().trim();

        if (result.length() > 200) {
            result = result.substring(0, 197) + "...";
        }

        return result.isEmpty() ? null : result;
    }

    private EmployerEntity getEmployerByUserid(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        EmployerMemberEntity membership = employerMemberRepository
                .findFirstByMemberIdAndActiveTrue(memberId)
                .orElseThrow(() -> new IllegalStateException("소속된 기업이 없습니다. 먼저 기업 정보를 등록해주세요."));

        return employerRepository.findById(membership.getEmployerId())
                .orElseThrow(() -> new IllegalStateException("기업 정보를 찾을 수 없습니다."));
    }

    private JobDTO toJobDTO(JobEntity job, EmployerEntity employer) {
        return JobDTO.builder()
                .jobId(job.getId())
                .jobUid(String.valueOf(job.getId()))
                .title(job.getTitle())
                .description(job.getDescription())
                .summary(job.getSummary())
                .status(job.getStatus())
                .location(job.getLocation())
                .salaryText(job.getSalaryText())
                .salaryDisplay(formatSalary(job.getSalaryText()))
                // ✅ String[] 그대로 내려줌
                .stack(cleanStackArray(job.getStack()))
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .updatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null)
                .companyName(employer.getName())
                .companyLogoUrl(employer.getLogoUrl())
                .build();
    }

    private String formatSalary(Long salary) {
        if (salary == null || salary <= 0) {
            return null;
        }

        long man = salary / 10000;

        if (man >= 10000) {
            long uk = man / 10000;
            long remainMan = man % 10000;
            if (remainMan > 0) {
                return String.format("%,d억 %,d만원", uk, remainMan);
            }
            return String.format("%,d억원", uk);
        }

        return String.format("%,d만원", man);
    }

    // ===== 기술 스택 매칭 기능 =====

    public JobListResponseDTO getPublicJobsWithMatch(int page, int size, String keyword,
                                                     String stack, String location, Long memberId) {
        JobListResponseDTO baseResponse = getPublicJobs(page, size, keyword, stack, location);

        if (memberId == null) {
            return baseResponse;
        }

        Set<String> candidateSkills;
        try {
            candidateSkills = resumeSkillService.getSkillsByMemberId(memberId);
            if (candidateSkills == null) {
                candidateSkills = Collections.emptySet();
            }
            log.debug("지원자 기술 스택 조회 성공 (memberId: {}): {}", memberId, candidateSkills);
        } catch (Exception e) {
            log.warn("지원자 기술 스택 조회 실패 (memberId: {}): {}", memberId, e.getMessage());
            candidateSkills = Collections.emptySet();
        }

        if (candidateSkills.isEmpty()) {
            log.debug("지원자에게 등록된 기술 스택이 없습니다 (memberId: {}). 매칭 정보 없이 반환합니다.", memberId);
            return baseResponse;
        }

        final Set<String> finalCandidateSkills = candidateSkills;

        List<JobDTO> jobsWithMatch = baseResponse.getJobs().stream()
                .map(job -> {
                    try {
                        JobMatchInfoDTO matchInfo = calculateMatchInfo(
                                job.getStack(),             // ✅ String[]
                                finalCandidateSkills,
                                null,
                                null,
                                false
                        );
                        job.setMatchInfo(matchInfo);
                    } catch (Exception e) {
                        log.debug("공고 {} 매칭 계산 실패: {}", job.getJobId(), e.getMessage());
                    }
                    return job;
                })
                .sorted((j1, j2) -> {
                    int rate1 = j1.getMatchInfo() != null ? j1.getMatchInfo().getOverallMatchRate() : 0;
                    int rate2 = j2.getMatchInfo() != null ? j2.getMatchInfo().getOverallMatchRate() : 0;
                    return Integer.compare(rate2, rate1);
                })
                .collect(Collectors.toList());

        log.info("매칭률 정렬 완료 - 로그인 사용자(memberId: {}), 공고 {}건", memberId, jobsWithMatch.size());

        return JobListResponseDTO.builder()
                .jobs(jobsWithMatch)
                .page(baseResponse.getPage())
                .size(baseResponse.getSize())
                .totalElements(baseResponse.getTotalElements())
                .totalPages(baseResponse.getTotalPages())
                .build();
    }

    @Transactional
    public JobDTO getPublicJobWithMatch(Long jobId, Long memberId) {
        JobDTO job = getPublicJob(jobId);

        if (memberId == null) {
            return job;
        }

        try {
            Set<String> candidateSkills;
            try {
                candidateSkills = resumeSkillService.getSkillsByMemberId(memberId);
                if (candidateSkills == null) {
                    candidateSkills = Collections.emptySet();
                }
                log.debug("[상세] 지원자 기술 스택 (memberId: {}): {}", memberId, candidateSkills);
            } catch (Exception e) {
                log.warn("지원자 기술 스택 조회 실패 (memberId: {}): {}", memberId, e.getMessage());
                candidateSkills = Collections.emptySet();
            }

            if (candidateSkills.isEmpty()) {
                log.debug("[상세] 지원자에게 등록된 기술 스택이 없습니다 (memberId: {})", memberId);
                return job;
            }

            log.debug("[상세] 공고 기술 스택 (jobId: {}): {}", jobId, Arrays.toString(job.getStack()));

            Map<String, Integer> proficiencyMap = resumeSkillService.getSkillProficiencyMap(memberId);
            log.debug("[상세] 지원자 숙련도 맵: {}", proficiencyMap);

            JobMatchInfoDTO matchInfo = calculateMatchInfoWithProficiency(
                    job.getStack(),                 // ✅ String[]
                    candidateSkills,
                    proficiencyMap,
                    null,
                    null,
                    false
            );
            job.setMatchInfo(matchInfo);
            log.debug("[상세] 매칭 결과 - 매칭률: {}%, 매칭 스택: {}",
                    matchInfo.getOverallMatchRate(), matchInfo.getMatchedStacks());
        } catch (Exception e) {
            log.warn("매칭 정보 계산 실패 (jobId: {}, memberId: {}): {}", jobId, memberId, e.getMessage());
        }

        return job;
    }

    // ===== 매칭 계산 (String[] 기반) =====

    public JobMatchInfoDTO calculateMatchInfo(String[] jobStack, Set<String> candidateSkills,
                                              JobEntity jobEntity, Long memberId) {
        return calculateMatchInfo(jobStack, candidateSkills, jobEntity, memberId, true);
    }

    private JobMatchInfoDTO calculateMatchInfo(String[] jobStack, Set<String> candidateSkills,
                                               JobEntity jobEntity, Long memberId,
                                               boolean includeVectorMatch) {

        List<String> requiredStacks = parseStackArray(jobStack);

        if (requiredStacks.isEmpty()) {
            JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                    .stackMatchRate(0)
                    .vectorMatchRate(0)
                    .requiredStacks(Collections.emptyList())
                    .matchedStacks(Collections.emptyList())
                    .missingStacks(Collections.emptyList())
                    .matchLevel("LOW")
                    .build();
            matchInfo.setOverallMatchRate(0);
            return matchInfo;
        }

        if (candidateSkills == null) {
            candidateSkills = Collections.emptySet();
        }

        Set<String> normalizedCandidateSkills = candidateSkills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<String> matchedStacks = new ArrayList<>();
        List<String> missingStacks = new ArrayList<>();

        for (String required : requiredStacks) {
            String normalizedRequired = required.toLowerCase().trim();

            boolean matched = normalizedCandidateSkills.stream()
                    .anyMatch(candidate ->
                            candidate.equals(normalizedRequired) ||
                                    candidate.contains(normalizedRequired) ||
                                    normalizedRequired.contains(candidate) ||
                                    isSynonymMatch(normalizedRequired, candidate)
                    );

            if (matched) {
                matchedStacks.add(required);
            } else {
                missingStacks.add(required);
            }
        }

        int stackMatchRate = requiredStacks.isEmpty() ? 0 :
                (int) Math.round((double) matchedStacks.size() / requiredStacks.size() * 100);

        int vectorMatchRate = 0;
        if (includeVectorMatch && jobEntity != null && memberId != null) {
            try {
                vectorMatchRate = calculateVectorSimilarity(jobEntity, memberId);
            } catch (Exception e) {
                log.warn("벡터 유사도 계산 중 오류 발생: {}", e.getMessage());
                vectorMatchRate = 0;
            }
        }

        int overallMatchRate;
        if (includeVectorMatch && vectorMatchRate > 0) {
            overallMatchRate = (int) Math.round(stackMatchRate * 0.4 + vectorMatchRate * 0.6);
        } else {
            overallMatchRate = stackMatchRate;
        }

        String matchLevel = JobMatchInfoDTO.calculateMatchLevel(overallMatchRate);

        JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                .stackMatchRate(stackMatchRate)
                .vectorMatchRate(vectorMatchRate)
                .requiredStacks(requiredStacks)
                .matchedStacks(matchedStacks)
                .missingStacks(missingStacks)
                .matchLevel(matchLevel)
                .build();

        matchInfo.setOverallMatchRate(overallMatchRate);
        return matchInfo;
    }

    /**
     * (기존 하위호환) String 기반도 유지
     */
    public JobMatchInfoDTO calculateMatchInfo(String jobStack, Set<String> candidateSkills,
                                              JobEntity jobEntity, Long memberId) {
        return calculateMatchInfo(jobStack, candidateSkills, jobEntity, memberId, true);
    }

    private JobMatchInfoDTO calculateMatchInfo(String jobStack, Set<String> candidateSkills,
                                               JobEntity jobEntity, Long memberId,
                                               boolean includeVectorMatch) {
        if (jobStack == null || jobStack.isBlank()) {
            JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                    .stackMatchRate(0)
                    .vectorMatchRate(0)
                    .requiredStacks(Collections.emptyList())
                    .matchedStacks(Collections.emptyList())
                    .missingStacks(Collections.emptyList())
                    .matchLevel("LOW")
                    .build();
            matchInfo.setOverallMatchRate(0);
            return matchInfo;
        }

        List<String> requiredStacks = parseStackString(jobStack);

        if (requiredStacks.isEmpty()) {
            JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                    .stackMatchRate(0)
                    .vectorMatchRate(0)
                    .requiredStacks(Collections.emptyList())
                    .matchedStacks(Collections.emptyList())
                    .missingStacks(Collections.emptyList())
                    .matchLevel("LOW")
                    .build();
            matchInfo.setOverallMatchRate(0);
            return matchInfo;
        }

        if (candidateSkills == null) {
            candidateSkills = Collections.emptySet();
        }

        Set<String> normalizedCandidateSkills = candidateSkills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<String> matchedStacks = new ArrayList<>();
        List<String> missingStacks = new ArrayList<>();

        for (String required : requiredStacks) {
            String normalizedRequired = required.toLowerCase().trim();

            boolean matched = normalizedCandidateSkills.stream()
                    .anyMatch(candidate ->
                            candidate.equals(normalizedRequired) ||
                                    candidate.contains(normalizedRequired) ||
                                    normalizedRequired.contains(candidate) ||
                                    isSynonymMatch(normalizedRequired, candidate)
                    );

            if (matched) matchedStacks.add(required);
            else missingStacks.add(required);
        }

        int stackMatchRate = requiredStacks.isEmpty() ? 0 :
                (int) Math.round((double) matchedStacks.size() / requiredStacks.size() * 100);

        int vectorMatchRate = 0;
        if (includeVectorMatch && jobEntity != null && memberId != null) {
            try {
                vectorMatchRate = calculateVectorSimilarity(jobEntity, memberId);
            } catch (Exception e) {
                log.warn("벡터 유사도 계산 중 오류 발생: {}", e.getMessage());
                vectorMatchRate = 0;
            }
        }

        int overallMatchRate;
        if (includeVectorMatch && vectorMatchRate > 0) {
            overallMatchRate = (int) Math.round(stackMatchRate * 0.4 + vectorMatchRate * 0.6);
        } else {
            overallMatchRate = stackMatchRate;
        }

        String matchLevel = JobMatchInfoDTO.calculateMatchLevel(overallMatchRate);

        JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                .stackMatchRate(stackMatchRate)
                .vectorMatchRate(vectorMatchRate)
                .requiredStacks(requiredStacks)
                .matchedStacks(matchedStacks)
                .missingStacks(missingStacks)
                .matchLevel(matchLevel)
                .build();

        matchInfo.setOverallMatchRate(overallMatchRate);
        return matchInfo;
    }

    @Deprecated
    public JobMatchInfoDTO calculateMatchInfo(String jobStack, Set<String> candidateSkills) {
        return calculateMatchInfo(jobStack, candidateSkills, null, null, false);
    }

    private JobMatchInfoDTO calculateMatchInfoWithProficiency(String[] jobStack, Set<String> candidateSkills,
                                                              Map<String, Integer> proficiencyMap,
                                                              JobEntity jobEntity, Long memberId,
                                                              boolean includeVectorMatch) {
        List<String> requiredStacks = parseStackArray(jobStack);
        if (requiredStacks.isEmpty()) {
            return createEmptyMatchInfo();
        }

        if (candidateSkills == null) {
            candidateSkills = Collections.emptySet();
        }
        if (proficiencyMap == null) {
            proficiencyMap = Collections.emptyMap();
        }

        Set<String> normalizedCandidateSkills = candidateSkills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<String> matchedStacks = new ArrayList<>();
        List<String> missingStacks = new ArrayList<>();
        List<JobMatchInfoDTO.SkillMatchDetail> skillDetails = new ArrayList<>();

        for (String required : requiredStacks) {
            String normalizedRequired = required.toLowerCase().trim();

            boolean matched = normalizedCandidateSkills.stream()
                    .anyMatch(candidate ->
                            candidate.equals(normalizedRequired) ||
                                    candidate.contains(normalizedRequired) ||
                                    normalizedRequired.contains(candidate) ||
                                    isSynonymMatch(normalizedRequired, candidate)
                    );

            int proficiency = 0;
            if (matched) {
                matchedStacks.add(required);

                proficiency = proficiencyMap.getOrDefault(normalizedRequired, 1);
                if (proficiency == 0) {
                    for (String candidate : normalizedCandidateSkills) {
                        if (isSynonymMatch(normalizedRequired, candidate)) {
                            proficiency = proficiencyMap.getOrDefault(candidate, 1);
                            if (proficiency > 0) break;
                        }
                    }
                }
                if (proficiency == 0) proficiency = 1;
            } else {
                missingStacks.add(required);
            }

            skillDetails.add(JobMatchInfoDTO.SkillMatchDetail.of(required, matched, proficiency));
        }

        int stackMatchRate = (int) Math.round((matchedStacks.size() / (double) requiredStacks.size()) * 100);

        int vectorMatchRate = 0;
        if (includeVectorMatch && jobEntity != null && memberId != null) {
            try {
                vectorMatchRate = calculateVectorSimilarity(jobEntity, memberId);
            } catch (Exception e) {
                log.warn("벡터 유사도 계산 중 오류 발생: {}", e.getMessage());
            }
        }

        int overallMatchRate;
        if (includeVectorMatch && vectorMatchRate > 0) {
            overallMatchRate = (int) Math.round(stackMatchRate * 0.6 + vectorMatchRate * 0.4);
        } else {
            overallMatchRate = stackMatchRate;
        }

        String matchLevel = JobMatchInfoDTO.calculateMatchLevel(overallMatchRate);

        List<String> candidateStackList = new ArrayList<>(candidateSkills);

        JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                .stackMatchRate(stackMatchRate)
                .vectorMatchRate(vectorMatchRate)
                .requiredStacks(requiredStacks)
                .matchedStacks(matchedStacks)
                .missingStacks(missingStacks)
                .matchLevel(matchLevel)
                .skillDetails(skillDetails)
                .candidateStacks(candidateStackList)
                .skillProficiencyMap(proficiencyMap)
                .build();

        matchInfo.setOverallMatchRate(overallMatchRate);
        return matchInfo;
    }

    private JobMatchInfoDTO createEmptyMatchInfo() {
        JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                .stackMatchRate(0)
                .vectorMatchRate(0)
                .requiredStacks(Collections.emptyList())
                .matchedStacks(Collections.emptyList())
                .missingStacks(Collections.emptyList())
                .skillDetails(Collections.emptyList())
                .candidateStacks(Collections.emptyList())
                .skillProficiencyMap(Collections.emptyMap())
                .matchLevel("LOW")
                .build();
        matchInfo.setOverallMatchRate(0);
        return matchInfo;
    }

    private int calculateVectorSimilarity(JobEntity jobEntity, Long memberId) {
        try {
            if (memberId == null) {
                log.debug("memberId가 null입니다.");
                return 0;
            }

            if (jobEntity == null) {
                log.debug("jobEntity가 null입니다.");
                return 0;
            }

            List<Double> jobEmbedding = jobEntity.getEmbedding();
            if (jobEmbedding == null || jobEmbedding.isEmpty()) {
                log.debug("채용공고 {}의 벡터가 없습니다.", jobEntity.getId());
                return 0;
            }

            Optional<Resume> resumeOpt = resumeRepository.findByUser_IdAndPrimaryTrue(memberId);
            if (resumeOpt.isEmpty()) {
                log.debug("회원 {}의 대표 이력서가 없습니다.", memberId);
                return 0;
            }

            Resume resume = resumeOpt.get();
            List<Double> resumeEmbedding = resume.getEmbedding();
            if (resumeEmbedding == null || resumeEmbedding.isEmpty()) {
                log.debug("이력서 {}의 벡터가 없습니다.", resume.getId());
                return 0;
            }

            double cosineSimilarity = calculateCosineSimilarity(jobEmbedding, resumeEmbedding);

            return (int) Math.round(Math.max(0, Math.min(1, cosineSimilarity)) * 100);
        } catch (Exception e) {
            log.warn("벡터 유사도 계산 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }

    private double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("벡터 차원이 일치하지 않습니다.");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            double v1 = vector1.get(i);
            double v2 = vector2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    // ===== stack 파서 (String[] / String 둘 다 지원) =====

    private List<String> parseStackArray(String[] stackArray) {
        if (stackArray == null || stackArray.length == 0) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>();
        for (String raw : stackArray) {
            if (raw == null) continue;
            String s = raw.trim();
            if (s.isBlank()) continue;

            // "{AWS,Vue}" 같은 문자열이 섞인 경우도 처리
            if (s.startsWith("{") && s.endsWith("}")) {
                out.addAll(parseStackString(s));
                continue;
            }

            String[] parts = s.split("[,/;|]");
            for (String part : parts) {
                String p = part.trim().replaceAll("^\"|\"$", "");
                if (!p.isBlank()) out.add(p);
            }
        }

        return out;
    }

    private List<String> parseStackString(String stackString) {
        if (stackString == null || stackString.isBlank()) {
            return Collections.emptyList();
        }

        String cleaned = stackString.replaceAll("^\\{|\\}$", "");

        return Arrays.stream(cleaned.split("[,/;|]"))
                .map(String::trim)
                .map(s -> s.replaceAll("^\"|\"$", ""))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.isBlank())
                .collect(Collectors.toList());
    }

    private boolean isSynonymMatch(String stack1, String stack2) {
        Map<String, Set<String>> synonyms = Map.ofEntries(
                Map.entry("javascript", Set.of("js", "자바스크립트")),
                Map.entry("typescript", Set.of("ts", "타입스크립트")),
                Map.entry("react", Set.of("reactjs", "react.js", "리액트")),
                Map.entry("vue", Set.of("vuejs", "vue.js", "뷰")),
                Map.entry("angular", Set.of("angularjs", "angular.js", "앵귤러")),
                Map.entry("node", Set.of("nodejs", "node.js", "노드")),
                Map.entry("spring", Set.of("springboot", "spring boot", "스프링")),
                Map.entry("java", Set.of("자바")),
                Map.entry("python", Set.of("파이썬")),
                Map.entry("kotlin", Set.of("코틀린")),
                Map.entry("postgresql", Set.of("postgres", "포스트그레스")),
                Map.entry("mysql", Set.of("마이에스큐엘")),
                Map.entry("mongodb", Set.of("mongo", "몽고디비")),
                Map.entry("aws", Set.of("amazon web services", "아마존")),
                Map.entry("gcp", Set.of("google cloud", "구글클라우드")),
                Map.entry("docker", Set.of("도커")),
                Map.entry("kubernetes", Set.of("k8s", "쿠버네티스"))
        );

        for (Map.Entry<String, Set<String>> entry : synonyms.entrySet()) {
            Set<String> allVariants = new HashSet<>(entry.getValue());
            allVariants.add(entry.getKey());

            if (allVariants.contains(stack1) && allVariants.contains(stack2)) {
                return true;
            }
        }

        return false;
    }
}
