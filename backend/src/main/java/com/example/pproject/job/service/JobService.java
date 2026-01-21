package com.example.pproject.job.service;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.dto.*;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
import com.example.pproject.outbox.producer.OutboxEventProducer;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    
    // Resume 모듈에서 구현 필요 - Optional로 주입받아 없으면 매칭 기능 비활성화
    private final Optional<CandidateSkillProvider> candidateSkillProvider;

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

        // 본인 기업의 채용공고인지 확인
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

        // 기업 상태 확인 (ACTIVE만 등록 가능)
        if (!"ACTIVE".equals(employer.getStatus())) {
            throw new IllegalStateException("기업이 활성 상태가 아닙니다.");
        }

        // description에서 자동 요약 생성
        String summary = generateSummary(dto.getDescription());

        JobEntity job = JobEntity.builder()
                .employerId(employer.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .summary(summary)  // 자동 생성된 요약
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .location(dto.getLocation())
                .salaryText(dto.getSalaryText())
                .stack(dto.getStack())
                .requiredQuestions(dto.getRequiredQuestions())
                .build();

        jobRepository.save(job);
        log.info("채용공고 생성: {} (기업: {}, 요약: {})", job.getTitle(), employer.getName(), summary);

        // 알림 이벤트 발행
        try {
            outboxEventProducer.publishJobPostingCreatedEvent(
                    job.getId(),
                    user.getId(),
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

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        String oldStatus = job.getStatus();

        // 업데이트
        if (dto.getTitle() != null) job.setTitle(dto.getTitle());
        if (dto.getDescription() != null) {
            job.setDescription(dto.getDescription());
            // description이 변경되면 요약도 다시 생성
            job.setSummary(generateSummary(dto.getDescription()));
        }
        if (dto.getStatus() != null) job.setStatus(dto.getStatus());
        if (dto.getLocation() != null) job.setLocation(dto.getLocation());
        if (dto.getSalaryText() != null) job.setSalaryText(dto.getSalaryText());
        if (dto.getStack() != null) job.setStack(dto.getStack());
        if (dto.getRequiredQuestions() != null) job.setRequiredQuestions(dto.getRequiredQuestions());

        jobRepository.save(job);
        log.info("채용공고 수정: {}", job.getTitle());

        // 알림 이벤트 발행
        try {
            // 상태가 변경된 경우 상태 변경 알림
            if (dto.getStatus() != null && !dto.getStatus().equals(oldStatus)) {
                outboxEventProducer.publishJobPostingStatusChangedEvent(
                        job.getId(),
                        user.getId(),
                        job.getTitle(),
                        employer.getName(),
                        oldStatus,
                        dto.getStatus()
                );
            } else {
                // 일반 수정 알림
                outboxEventProducer.publishJobPostingUpdatedEvent(
                        job.getId(),
                        user.getId(),
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

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        String jobTitle = job.getTitle(); // 삭제 전 제목 저장

        job.setDeletedAt(LocalDateTime.now());
        job.setStatus("CLOSED");
        jobRepository.save(job);
        log.info("채용공고 삭제: {}", jobTitle);

        // 알림 이벤트 발행
        try {
            outboxEventProducer.publishJobPostingDeletedEvent(
                    job.getId(),
                    user.getId(),
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
     * 공개 채용공고 목록 조회 (status='OPEN')
     * - 대소문자 구분 없이 검색
     * - 한글 기술스택 검색 지원 (자바 → Java)
     */
    public JobListResponseDTO getPublicJobs(int page, int size, String keyword, String stack, String location) {
        Page<JobEntity> jobPage;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (keyword != null && !keyword.isBlank()) {
            // 키워드 검색 (한글 → 영어 변환 포함)
            String searchKeyword = convertKoreanToEnglish(keyword.trim());
            log.info("검색 키워드 변환: '{}' → '{}'", keyword.trim(), searchKeyword);
            jobPage = jobRepository.searchPublicJobs(searchKeyword, pageRequest);
        } else if (stack != null && !stack.isBlank()) {
            // 스택 필터 (한글 → 영어 변환 포함)
            String searchStack = convertKoreanToEnglish(stack.trim());
            log.info("스택 필터 변환: '{}' → '{}'", stack.trim(), searchStack);
            jobPage = jobRepository.findPublicJobsByStack(searchStack, pageRequest);
        } else if (location != null && !location.isBlank()) {
            // 지역 필터
            jobPage = jobRepository.findPublicJobsByLocation(location.trim(), pageRequest);
        } else {
            // 전체 조회
            jobPage = jobRepository.findPublicJobs(pageRequest);
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

    /**
     * 한글 기술스택을 영어로 변환
     * - 완전 일치하는 한글 키워드가 있으면 영어로 변환
     * - 없으면 원본 그대로 반환 (대소문자 구분 없는 검색은 Repository에서 처리)
     */
    private String convertKoreanToEnglish(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return keyword;
        }
        
        // 1. 완전 일치하는 한글 키워드 찾기 (대소문자 무시)
        String lowerKeyword = keyword.toLowerCase().trim();
        
        for (Map.Entry<String, String> entry : KOREAN_TO_ENGLISH_STACK.entrySet()) {
            if (entry.getKey().equals(lowerKeyword)) {
                return entry.getValue();
            }
        }
        
        // 2. 부분 일치도 체크 (한글 키워드가 포함된 경우)
        for (Map.Entry<String, String> entry : KOREAN_TO_ENGLISH_STACK.entrySet()) {
            if (lowerKeyword.contains(entry.getKey())) {
                // 한글 부분을 영어로 대체
                return keyword.replace(entry.getKey(), entry.getValue());
            }
        }
        
        // 3. 변환할 게 없으면 원본 반환
        return keyword;
    }

    /**
     * 공개 채용공고 상세 조회 (조회수 증가)
     */
    @Transactional
    public JobDTO getPublicJob(Long jobId) {
        JobEntity job = jobRepository.findPublicJobById(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 조회수 증가
        job.setViewCount(job.getViewCount() + 1);
        jobRepository.save(job);

        return toPublicJobDTO(job);
    }

    /**
     * 공개 채용공고 DTO 변환 (기업 정보 포함)
     */
    private JobDTO toPublicJobDTO(JobEntity job) {
        // 기업 정보 조회
        EmployerEntity employer = employerRepository.findById(job.getEmployerId())
                .orElse(null);

        return JobDTO.builder()
                .jobId(job.getId())
                .jobUid(String.valueOf(job.getId()))
                .title(job.getTitle())
                .description(job.getDescription())
                .summary(job.getSummary())
                .status(job.getStatus())
                .location(job.getLocation())
                .salaryText(job.getSalaryText())
                .stack(job.getStack())
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .updatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null)
                .companyName(employer != null ? employer.getName() : "알 수 없음")
                .companyLogoUrl(employer != null ? employer.getLogoUrl() : null)
                .build();
    }

    // === Helper Methods ===

    /**
     * description에서 자동 요약 생성 (첫 1~2문장, 최대 200자)
     */
    private String generateSummary(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        // 줄바꿈으로 문단 분리
        String cleaned = description.trim();
        
        // 첫 번째 문단 또는 첫 몇 문장 추출
        String[] paragraphs = cleaned.split("\\n\\n|\\r\\n\\r\\n");
        String firstParagraph = paragraphs[0].trim();
        
        // 문장 단위로 분리 (마침표, 느낌표, 물음표 기준)
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
        
        // 200자 초과 시 자르기
        if (result.length() > 200) {
            result = result.substring(0, 197) + "...";
        }
        
        return result.isEmpty() ? null : result;
    }

    /**
     * userid로 소속 기업 조회 (employer_member 통해)
     */
    private EmployerEntity getEmployerByUserid(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long memberId = user.getId().longValue();

        // employer_member에서 소속 기업 찾기
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
                .summary(job.getSummary())  // 요약 추가
                .status(job.getStatus())
                .location(job.getLocation())
                .salaryText(job.getSalaryText())
                .stack(job.getStack())
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .updatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null)
                .companyName(employer.getName())
                .companyLogoUrl(employer.getLogoUrl())
                .build();
    }

    // ===== 기술 스택 매칭 기능 =====

    /**
     * 공개 채용공고 목록 조회 (매칭 정보 포함)
     * - 로그인한 지원자의 경우 각 공고에 대한 매칭률 포함
     */
    public JobListResponseDTO getPublicJobsWithMatch(int page, int size, String keyword, 
                                                      String stack, String location, Long memberId) {
        JobListResponseDTO baseResponse = getPublicJobs(page, size, keyword, stack, location);
        
        // memberId가 없거나 CandidateSkillProvider가 없으면 매칭 정보 없이 반환
        if (memberId == null || candidateSkillProvider.isEmpty()) {
            return baseResponse;
        }
        
        // 지원자의 기술 스택 조회
        Set<String> candidateSkills = candidateSkillProvider.get().getSkillsByMemberId(memberId);
        
        if (candidateSkills.isEmpty()) {
            return baseResponse;
        }
        
        // 각 공고에 매칭 정보 추가
        List<JobDTO> jobsWithMatch = baseResponse.getJobs().stream()
                .map(job -> {
                    JobMatchInfoDTO matchInfo = calculateMatchInfo(job.getStack(), candidateSkills);
                    job.setMatchInfo(matchInfo);
                    return job;
                })
                .collect(Collectors.toList());
        
        return JobListResponseDTO.builder()
                .jobs(jobsWithMatch)
                .page(baseResponse.getPage())
                .size(baseResponse.getSize())
                .totalElements(baseResponse.getTotalElements())
                .totalPages(baseResponse.getTotalPages())
                .build();
    }

    /**
     * 공개 채용공고 상세 조회 (매칭 정보 포함)
     */
    @Transactional
    public JobDTO getPublicJobWithMatch(Long jobId, Long memberId) {
        JobDTO job = getPublicJob(jobId);
        
        // memberId가 없거나 CandidateSkillProvider가 없으면 매칭 정보 없이 반환
        if (memberId == null || candidateSkillProvider.isEmpty()) {
            return job;
        }
        
        // 지원자의 기술 스택 조회
        Set<String> candidateSkills = candidateSkillProvider.get().getSkillsByMemberId(memberId);
        
        if (!candidateSkills.isEmpty()) {
            JobMatchInfoDTO matchInfo = calculateMatchInfo(job.getStack(), candidateSkills);
            job.setMatchInfo(matchInfo);
        }
        
        return job;
    }

    /**
     * 채용공고 기술 스택과 지원자 기술 스택 간 매칭 정보 계산
     * 
     * @param jobStack 채용공고의 기술 스택 (쉼표로 구분된 문자열)
     * @param candidateSkills 지원자의 기술 스택 Set
     * @return 매칭 정보 DTO
     */
    public JobMatchInfoDTO calculateMatchInfo(String jobStack, Set<String> candidateSkills) {
        if (jobStack == null || jobStack.isBlank()) {
            return JobMatchInfoDTO.builder()
                    .matchRate(0)
                    .requiredStacks(Collections.emptyList())
                    .matchedStacks(Collections.emptyList())
                    .missingStacks(Collections.emptyList())
                    .matchLevel("LOW")
                    .build();
        }
        
        // 채용공고 요구 스택 파싱 (쉼표, 슬래시, 공백 등으로 구분)
        List<String> requiredStacks = parseStackString(jobStack);
        
        if (requiredStacks.isEmpty()) {
            return JobMatchInfoDTO.builder()
                    .matchRate(0)
                    .requiredStacks(Collections.emptyList())
                    .matchedStacks(Collections.emptyList())
                    .missingStacks(Collections.emptyList())
                    .matchLevel("LOW")
                    .build();
        }
        
        // 정규화된 지원자 스택 (소문자)
        Set<String> normalizedCandidateSkills = candidateSkills.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());
        
        // 매칭되는 스택 찾기
        List<String> matchedStacks = new ArrayList<>();
        List<String> missingStacks = new ArrayList<>();
        
        for (String required : requiredStacks) {
            String normalizedRequired = required.toLowerCase().trim();
            
            // 정확히 일치하거나, 부분 일치 확인
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
        
        // 매칭률 계산
        int matchRate = (int) Math.round((double) matchedStacks.size() / requiredStacks.size() * 100);
        String matchLevel = JobMatchInfoDTO.calculateMatchLevel(matchRate);
        
        log.debug("매칭 계산 - 요구: {}, 보유: {}, 일치: {}, 매칭률: {}%", 
                requiredStacks, candidateSkills, matchedStacks, matchRate);
        
        return JobMatchInfoDTO.builder()
                .matchRate(matchRate)
                .requiredStacks(requiredStacks)
                .matchedStacks(matchedStacks)
                .missingStacks(missingStacks)
                .matchLevel(matchLevel)
                .build();
    }

    /**
     * 기술 스택 문자열 파싱
     * - 쉼표, 슬래시, 세미콜론 등으로 구분된 문자열을 리스트로 변환
     */
    private List<String> parseStackString(String stackString) {
        if (stackString == null || stackString.isBlank()) {
            return Collections.emptyList();
        }
        
        // 다양한 구분자 지원: 쉼표, 슬래시, 세미콜론, 파이프
        return Arrays.stream(stackString.split("[,/;|]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 동의어 매칭 확인
     * - JavaScript ↔ JS, TypeScript ↔ TS 등
     */
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
