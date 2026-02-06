package com.example.pproject.job.service;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.entity.EmployerStatus;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.dto.*;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.entity.JobStatus;
import com.example.pproject.job.repository.JobEntityRepository;
import com.example.pproject.outbox.producer.OutboxEventProducer;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.resume.service.ResumeSkillService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import com.example.pproject.common.util.ArrayStringUtil;
import com.example.pproject.common.util.IndustryUtil;
import com.example.pproject.common.util.JobPositionUtil;
import com.example.pproject.ad.service.AdCampaignService;
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

    private final JobEntityRepository jobEntityRepository;
    private final EmployerRepository employerRepository;
    private final EmployerMemberRepository employerMemberRepository;
    private final UserRepository userRepository;
    private final OutboxEventProducer outboxEventProducer;
    private final ResumeRepository resumeRepository;
    private final ResumeSkillService resumeSkillService;
    private final AdCampaignService adCampaignService; // Cascade Delete 지원

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
            Map.entry("블록체인", "Blockchain"));

    /**
     * 기업의 채용공고 목록 조회
     */
    public JobListResponseDTO getJobsByEmployer(String userid, int page, int size) {
        EmployerEntity employer = getEmployerByUserid(userid);

        Page<JobEntity> jobPage = jobEntityRepository.findByEmployerIdAndNotDeleted(
                employer.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

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

        JobEntity job = jobEntityRepository.findByIdAndNotDeleted(jobId)
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
        EmployerStatus employerStatus = EmployerStatus.fromString(employer.getStatus());
        if (!employerStatus.canPostJob()) {
            throw new IllegalStateException("기업이 활성 상태가 아닙니다.");
        }

        // description에서 자동 요약 생성
        String summary = generateSummary(dto.getDescription());

        // 상태값 검증 및 기본값 설정
        String jobStatus = dto.getStatus() != null
                ? JobStatus.fromString(dto.getStatus()).name()
                : JobStatus.DRAFT.name();

        JobEntity job = JobEntity.builder()
                .employerId(employer.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .summary(summary) // 자동 생성된 요약
                .status(jobStatus)
                .location(dto.getLocation())
                .salaryText(dto.getSalaryText())
                .stack(ArrayStringUtil.stringToList(dto.getStack()))
                .requiredExperience(dto.getRequiredExperience() != null ? dto.getRequiredExperience() : 0)
                .recruitmentCapacity(dto.getRecruitmentCapacity() != null ? dto.getRecruitmentCapacity() : 0)
                .requiredQuestions(dto.getRequiredQuestions())
                .images(dto.getImages())
                .build();

        jobEntityRepository.save(job);
        log.info("채용공고 생성: {} (기업: {}, 요약: {})", job.getTitle(), employer.getName(), summary);

        // 알림 이벤트 발행
        try {
            outboxEventProducer.publishJobPostingCreatedEvent(
                    job.getId(),
                    user.getId().longValue(),
                    job.getTitle(),
                    employer.getName(),
                    job.getStatus());
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

        JobEntity job = jobEntityRepository.findByIdAndNotDeleted(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        String oldStatus = job.getStatus();

        // 업데이트
        if (dto.getTitle() != null)
            job.setTitle(dto.getTitle());
        if (dto.getDescription() != null) {
            job.setDescription(dto.getDescription());
            // description이 변경되면 요약도 다시 생성
            job.setSummary(generateSummary(dto.getDescription()));
        }
        if (dto.getStatus() != null) job.setStatus(dto.getStatus());
        if (dto.getLocation() != null) job.setLocation(dto.getLocation());
        if (dto.getSalaryText() != null) job.setSalaryText(dto.getSalaryText());
        if (dto.getStack() != null) job.setStack(ArrayStringUtil.stringToList(dto.getStack()));
        if (dto.getRequiredExperience() != null) job.setRequiredExperience(dto.getRequiredExperience());
        if (dto.getRecruitmentCapacity() != null) job.setRecruitmentCapacity(dto.getRecruitmentCapacity());
        if (dto.getRequiredQuestions() != null) job.setRequiredQuestions(dto.getRequiredQuestions());
        if (dto.getImages() != null) job.setImages(dto.getImages());
        if (dto.getStatus() != null)
            job.setStatus(dto.getStatus());
        if (dto.getLocation() != null)
            job.setLocation(dto.getLocation());
        if (dto.getSalaryText() != null)
            job.setSalaryText(dto.getSalaryText());
        if (dto.getStack() != null)
            job.setStack(ArrayStringUtil.stringToList(dto.getStack()));
        if (dto.getRequiredExperience() != null)
            job.setRequiredExperience(dto.getRequiredExperience());
        if (dto.getRecruitmentCapacity() != null)
            job.setRecruitmentCapacity(dto.getRecruitmentCapacity());
        if (dto.getRequiredQuestions() != null)
            job.setRequiredQuestions(dto.getRequiredQuestions());

        jobEntityRepository.save(job);
        log.info("채용공고 수정: {}", job.getTitle());

        // 알림 이벤트 발행
        try {
            // 상태가 변경된 경우 상태 변경 알림
            if (dto.getStatus() != null && !dto.getStatus().equals(oldStatus)) {
                outboxEventProducer.publishJobPostingStatusChangedEvent(
                        job.getId(),
                        user.getId().longValue(),
                        job.getTitle(),
                        employer.getName(),
                        oldStatus,
                        dto.getStatus());
            } else {
                // 일반 수정 알림
                outboxEventProducer.publishJobPostingUpdatedEvent(
                        job.getId(),
                        user.getId().longValue(),
                        job.getTitle(),
                        employer.getName());
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

        JobEntity job = jobEntityRepository.findByIdAndNotDeleted(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 본인 기업의 채용공고인지 확인
        if (!job.getEmployerId().equals(employer.getId())) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        String jobTitle = job.getTitle(); // 삭제 전 제목 저장

        // 연관된 광고 캠페인도 함께 삭제 (Cascade Delete)
        adCampaignService.deleteCampaignsByJobId(jobId);

        job.setDeletedAt(Instant.now());
        job.setStatus(JobStatus.CLOSED.name());
        jobEntityRepository.save(job);
        log.info("채용공고 삭제: {}", jobTitle);

        // 알림 이벤트 발행
        try {
            outboxEventProducer.publishJobPostingDeletedEvent(
                    job.getId(),
                    user.getId().longValue(),
                    jobTitle,
                    employer.getName());
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
     * 채용공고 필터 옵션 조회 (사용 가능한 스택/지역/포지션/업종 목록 - DB 기반)
     */
    public JobFilterOptionsDTO getFilterOptions() {
        List<String> stacks = jobEntityRepository.findDistinctStacks();
        List<String> locations = jobEntityRepository.findDistinctLocations();
        List<String> rawIndustries = jobEntityRepository.findDistinctIndustries();

        // null 값 제거 및 빈 문자열 제거
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

        // 포지션 목록: 실제 채용공고 스택에서 도출 (DB 기반)
        List<String> stackStrings = jobEntityRepository.findAllStacksAsStrings();
        List<List<String>> allStacks = stackStrings.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(JobPositionUtil::parseStackString)
                .filter(list -> !list.isEmpty())
                .collect(Collectors.toList());

        List<String> positionCategories = JobPositionUtil.deriveAvailablePositions(allStacks);

        // 서비스 분야(업종): 영어 → 한글 변환
        List<String> industries = IndustryUtil.convertToKoreanList(rawIndustries);

        log.info("필터 옵션 조회 - 스택: {}개, 지역: {}개, 포지션: {}개, 업종: {}개",
                stacks.size(), locations.size(), positionCategories.size(), industries.size());

        return JobFilterOptionsDTO.builder()
                .stacks(stacks)
                .locations(locations)
                .positionCategories(positionCategories)
                .industries(industries)
                .experienceOptions(JobFilterOptionsDTO.getDefaultExperienceOptions())
                .build();
    }

    /**
     * 현재 필터 조건에 따른 포지션별 채용공고 카운트 조회
     * - 각 포지션 카테고리별로 해당 공고 수를 반환
     * - 0개인 포지션도 포함 (프론트에서 필터링 가능)
     */
    public Map<String, Long> getPositionCounts(String keyword, String stack, String location,
                                                Integer minExperience, Integer maxExperience,
                                                Integer salaryMin, Integer salaryMax, String industry) {
        // 모든 포지션 카테고리
        List<String> allPositions = JobPositionUtil.getAllPositionCategories();
        Map<String, Long> counts = new LinkedHashMap<>();

        // 기본 필터 조건 변환 (한글 → 영어)
        String searchKeyword = (keyword != null && !keyword.isBlank())
                ? convertKoreanToEnglish(keyword.trim())
                : null;
        String searchStack = (stack != null && !stack.isBlank())
                ? convertKoreanToEnglish(stack.trim())
                : null;
        String searchLocation = (location != null && !location.isBlank())
                ? location.trim()
                : null;

        // 업종(industry) → 영어 키워드 변환
        String industryKeywords = null;
        if (industry != null && !industry.isBlank() && !"전체".equals(industry.trim())) {
            Set<String> allKeywords = new LinkedHashSet<>();
            for (String ind : industry.split(",")) {
                String trimmed = ind.trim();
                if (!trimmed.isEmpty() && !"전체".equals(trimmed)) {
                    List<String> engKeywords = IndustryUtil.getEnglishKeywords(trimmed);
                    engKeywords.forEach(kw -> allKeywords.add("%" + kw.toLowerCase() + "%"));
                }
            }
            if (!allKeywords.isEmpty()) {
                industryKeywords = String.join(",", allKeywords);
            }
        }

        // 각 포지션별로 카운트 조회
        for (String position : allPositions) {
            if ("전체".equals(position)) {
                // 전체는 포지션 필터 없이 카운트
                long totalCount = jobEntityRepository.countPublicJobsWithFilters(
                        searchKeyword, searchStack, searchLocation,
                        minExperience, maxExperience, null, industryKeywords,
                        salaryMin, salaryMax);
                counts.put(position, totalCount);
            } else {
                // 해당 포지션의 키워드로 카운트
                Set<String> keywords = JobPositionUtil.getKeywordsByPosition(position);
                if (!keywords.isEmpty()) {
                    String positionKeywords = keywords.stream()
                            .map(kw -> "%" + kw.toLowerCase() + "%")
                            .collect(Collectors.joining(","));
                    long count = jobEntityRepository.countPublicJobsWithFilters(
                            searchKeyword, searchStack, searchLocation,
                            minExperience, maxExperience, positionKeywords, industryKeywords,
                            salaryMin, salaryMax);
                    counts.put(position, count);
                } else {
                    counts.put(position, 0L);
                }
            }
        }

        log.info("포지션별 카운트 조회 완료 - 총 {}개 포지션", counts.size());
        return counts;
    }

    /**
     * 공개 채용공고 목록 조회 (status='OPEN')
     * - 대소문자 구분 없이 검색
     * - 한글 기술스택 검색 지원 (자바 → Java)
     * - 다중 필터 지원 (keyword + stack + location + experience + position + industry)
     */
    public JobListResponseDTO getPublicJobs(int page, int size, String keyword, String stack, String location) {
        return getPublicJobs(page, size, keyword, stack, location, null, null, null, null, null, null);
    }

    /**
     * 공개 채용공고 목록 조회 (다중 필터 지원)
     *
     * @param page          페이지 번호
     * @param size          페이지 크기
     * @param keyword       검색 키워드 (제목, 스택, 지역)
     * @param stack         기술 스택 필터
     * @param location      지역 필터
     * @param minExperience 최소 경력
     * @param maxExperience 최대 경력
     */
    public JobListResponseDTO getPublicJobs(int page, int size, String keyword, String stack,
            String location, Integer minExperience, Integer maxExperience) {
        return getPublicJobs(page, size, keyword, stack, location, minExperience, maxExperience, null, null);
                                             String location, Integer minExperience, Integer maxExperience) {
        return getPublicJobs(page, size, keyword, stack, location, minExperience, maxExperience, null, null, null, null);
    }

    /**
     * 공개 채용공고 목록 조회 (다중 필터 지원 + 포지션 필터)
     *
     * @param page          페이지 번호
     * @param size          페이지 크기
     * @param keyword       검색 키워드 (제목, 스택, 지역)
     * @param stack         기술 스택 필터
     * @param location      지역 필터
     * @param minExperience 최소 경력
     * @param maxExperience 최대 경력
     * @param position      포지션 필터 (쉼표 구분: "프론트엔드,백엔드")
     */
    public JobListResponseDTO getPublicJobs(int page, int size, String keyword, String stack,
            String location, Integer minExperience, Integer maxExperience,
            String position) {
        return getPublicJobs(page, size, keyword, stack, location, minExperience, maxExperience, position, null);
                                             String location, Integer minExperience, Integer maxExperience,
                                             String position) {
        return getPublicJobs(page, size, keyword, stack, location, minExperience, maxExperience, position, null, null, null);
    }

    /**
     * 공개 채용공고 목록 조회 (다중 필터 지원 + 포지션 + 업종 필터)
     *
     * @param page          페이지 번호
     * @param size          페이지 크기
     * @param keyword       검색 키워드 (제목, 스택, 지역)
     * @param stack         기술 스택 필터
     * @param location      지역 필터
     * @param minExperience 최소 경력
     * @param maxExperience 최대 경력
     * @param position      포지션 필터 (쉼표 구분: "프론트엔드,백엔드")
     * @param industry      업종/서비스 분야 필터 (쉼표 구분: "커머스,금융/핀테크")
     */
    public JobListResponseDTO getPublicJobs(int page, int size, String keyword, String stack,
            String location, Integer minExperience, Integer maxExperience,
            String position, String industry) {
                                             String location, Integer minExperience, Integer maxExperience,
                                             String position, String industry,
                                             Integer salaryMin, Integer salaryMax) {
        // 네이티브 쿼리용 (정렬은 쿼리 내부에서 처리하므로 제외)
        PageRequest pageRequest = PageRequest.of(page, size);

        // 한글 → 영어 변환
        String searchKeyword = (keyword != null && !keyword.isBlank())
                ? convertKoreanToEnglish(keyword.trim())
                : null;
        String searchStack = (stack != null && !stack.isBlank())
                ? convertKoreanToEnglish(stack.trim())
                : null;
        String searchLocation = (location != null && !location.isBlank())
                ? location.trim()
                : null;

        // 포지션 → 키워드 변환 (예: "프론트엔드" → "%react%,%vue%,%angular%,...")
        String positionKeywords = null;
        if (position != null && !position.isBlank() && !"전체".equals(position.trim())) {
            Set<String> keywords = JobPositionUtil.getKeywordsByPositions(position);
            if (!keywords.isEmpty()) {
                // SQL LIKE ANY 패턴용으로 변환: "react" -> "%react%"
                positionKeywords = keywords.stream()
                        .map(kw -> "%" + kw.toLowerCase() + "%")
                        .collect(Collectors.joining(","));
                log.info("포지션 필터 변환: '{}' → 키워드 {}개", position, keywords.size());
            }
        }

        // 업종(industry) → 영어 키워드 변환 (예: "커머스" →
        // "%e-commerce%,%commerce%,%ecommerce%,...")
        String industryKeywords = null;
        if (industry != null && !industry.isBlank() && !"전체".equals(industry.trim())) {
            Set<String> allKeywords = new LinkedHashSet<>();
            for (String ind : industry.split(",")) {
                String trimmed = ind.trim();
                if (!trimmed.isEmpty() && !"전체".equals(trimmed)) {
                    // 한글 업종명으로 영어 키워드 목록 조회
                    List<String> engKeywords = IndustryUtil.getEnglishKeywords(trimmed);
                    engKeywords.forEach(kw -> allKeywords.add("%" + kw.toLowerCase() + "%"));
                }
            }
            if (!allKeywords.isEmpty()) {
                industryKeywords = String.join(",", allKeywords);
                log.info("업종 필터 변환: '{}' → 키워드 {}개", industry, allKeywords.size());
            }
        }

        if (searchKeyword != null) {
            log.info("검색 키워드 변환: '{}' → '{}'", keyword.trim(), searchKeyword);
        }
        if (searchStack != null) {
            log.info("스택 필터 변환: '{}' → '{}'", stack.trim(), searchStack);
        }

        // 다중 필터 적용 (포지션 키워드 + 업종 키워드 포함)
        Page<JobEntity> jobPage = jobEntityRepository.findPublicJobsWithFilters(
                searchKeyword,
                searchStack,
                searchLocation,
                minExperience,
                maxExperience,
                positionKeywords,
                industryKeywords,
                pageRequest);
                salaryMin,
                salaryMax,
                pageRequest
        );

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
        JobEntity job = jobEntityRepository.findPublicJobById(jobId)
                .orElseThrow(() -> new IllegalStateException("채용공고를 찾을 수 없습니다."));

        // 조회수 증가 (별도 쿼리로 처리하여 embedding 필드 업데이트 방지)
        jobEntityRepository.incrementViewCount(jobId);

        // DTO 변환 시 조회수 +1 반영
        JobDTO dto = toPublicJobDTO(job);
        dto.setViewCount(job.getViewCount() + 1);

        return dto;
    }

    /**
     * 공개 채용공고 DTO 변환 (기업 정보 포함)
     */
    public JobDTO toPublicJobDTO(JobEntity job) {
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
                .salaryDisplay(formatSalary(job.getSalaryText()))
                .stack(ArrayStringUtil.listToString(job.getStack()))
                .position(JobPositionUtil.derivePosition(job.getStack()))
                .requiredExperience(job.getRequiredExperience())
                .recruitmentCapacity(job.getRecruitmentCapacity())
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .updatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null)
                .companyName(employer != null ? employer.getName() : "알 수 없음")
                .companyLogoUrl(employer != null ? employer.getLogoUrl() : null)
                .images(job.getImages())
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
                .summary(job.getSummary()) // 요약 추가
                .status(job.getStatus())
                .location(job.getLocation())
                .salaryText(job.getSalaryText())
                .salaryDisplay(formatSalary(job.getSalaryText()))
                .stack(ArrayStringUtil.listToString(job.getStack()))
                .position(JobPositionUtil.derivePosition(job.getStack()))
                .requiredExperience(job.getRequiredExperience())
                .recruitmentCapacity(job.getRecruitmentCapacity())
                .viewCount(job.getViewCount() != null ? job.getViewCount() : 0)
                .applicationCount(job.getApplicationCount() != null ? job.getApplicationCount() : 0)
                .createdAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null)
                .updatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null)
                .companyName(employer.getName())
                .companyLogoUrl(employer.getLogoUrl())
                .images(job.getImages())
                .build();
    }

    /**
     * 급여 정보 표시용 포맷
     * salaryText가 String으로 변경되어 그대로 반환
     */
    public String formatSalary(String salaryText) {
        if (salaryText == null || salaryText.isBlank()) {
            return null;
        }
        return salaryText;
    }

    // ===== 기술 스택 매칭 기능 =====

    /**
     * 공개 채용공고 목록 조회 (매칭 정보 포함, 매칭률 순 정렬)
     * - 로그인한 지원자의 경우 각 공고에 대한 매칭률 포함
     * - 매칭률이 높은 공고가 상단에 표시됨
     * - 다중 필터 지원 (keyword + stack + location + experience + position + industry)
     */
    public JobListResponseDTO getPublicJobsWithMatch(int page, int size, String keyword,
            String stack, String location,
            Integer minExperience, Integer maxExperience,
            String position, String industry, Long memberId) {
        JobListResponseDTO baseResponse = getPublicJobs(page, size, keyword, stack, location,
                minExperience, maxExperience, position, industry);

        // memberId가 없거나 CandidateSkillProvider가 없으면 매칭 정보 없이 반환
        if (memberId == null) {
            return baseResponse;
        }

        // 지원자의 기술 스택 조회 (예외 발생 시 빈 Set 사용)
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

        // final 변수로 복사 (stream 내부에서 사용하기 위해)
        final Set<String> finalCandidateSkills = candidateSkills;

        // 각 공고에 매칭 정보 추가 (개별 오류는 무시)
        List<JobDTO> jobsWithMatch = baseResponse.getJobs().stream()
                .map(job -> {
                    try {
                        // 스택 매칭만 계산 (벡터 매칭은 목록에서 생략 - 성능상 이유)
                        JobMatchInfoDTO matchInfo = calculateMatchInfo(
                                job.getStack(),
                                finalCandidateSkills,
                                null, // 목록에서는 벡터 매칭 생략
                                null,
                                false // 벡터 매칭 비활성화
                        );
                        job.setMatchInfo(matchInfo);
                    } catch (Exception e) {
                        log.debug("공고 {} 매칭 계산 실패: {}", job.getJobId(), e.getMessage());
                    }
                    return job;
                })
                // 매칭률 높은 순으로 정렬 (매칭 정보가 없으면 0으로 처리)
                .sorted((j1, j2) -> {
                    int rate1 = j1.getMatchInfo() != null ? j1.getMatchInfo().getOverallMatchRate() : 0;
                    int rate2 = j2.getMatchInfo() != null ? j2.getMatchInfo().getOverallMatchRate() : 0;
                    return Integer.compare(rate2, rate1); // 내림차순
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

    /**
     * 공개 채용공고 상세 조회 (매칭 정보 포함)
     */
    @Transactional
    public JobDTO getPublicJobWithMatch(Long jobId, Long memberId) {
        JobDTO job = getPublicJob(jobId);

        // memberId가 없거나 CandidateSkillProvider가 없으면 매칭 정보 없이 반환
        if (memberId == null) {
            return job;
        }

        try {
            // JobEntity 조회 (경력 매칭용)
            JobEntity jobEntity = jobEntityRepository.findPublicJobById(jobId).orElse(null);

            // 지원자의 기술 스택 조회
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

            log.debug("[상세] 공고 기술 스택 (jobId: {}): {}", jobId, job.getStack());

            // 숙련도 맵 조회
            Map<String, Integer> proficiencyMap = resumeSkillService.getSkillProficiencyMap(memberId);
            log.debug("[상세] 지원자 숙련도 맵: {}", proficiencyMap);

            // 매칭 정보 계산 (숙련도 기반 + 경력 매칭, 벡터 매칭 포함)
            JobMatchInfoDTO matchInfo = calculateMatchInfoWithProficiency(
                    job.getStack(),
                    candidateSkills,
                    proficiencyMap,
                    jobEntity, // 경력 및 벡터 매칭용
                    memberId,
                    true, null, null);
            job.setMatchInfo(matchInfo);
            log.debug("[상세] 매칭 결과 - 매칭률: {}%, 스택매칭: {}%, 경력매칭: {}%",
                    matchInfo.getOverallMatchRate(), matchInfo.getStackMatchRate(), matchInfo.getExperienceMatchRate());
        } catch (Exception e) {
            log.warn("매칭 정보 계산 실패 (jobId: {}, memberId: {}): {}", jobId, memberId, e.getMessage());
            // 매칭 정보 없이 반환
        }

        return job;
    }

    /**
     * 채용공고와 지원자 간 종합 매칭 정보 계산
     * - 기술 스택 매칭 + 벡터 유사도 매칭
     * 
     * @param jobStack        채용공고의 기술 스택 (쉼표로 구분된 문자열)
     * @param candidateSkills 지원자의 기술 스택 Set
     * @param jobEntity       채용공고 엔티티 (벡터 정보 포함)
     * @param memberId        지원자 회원 ID (이력서 벡터 조회용)
     * @return 매칭 정보 DTO
     */
    public JobMatchInfoDTO calculateMatchInfo(String jobStack, Set<String> candidateSkills,
            JobEntity jobEntity, Long memberId) {
        // 기존 메서드 호출 (하위 호환성)
        return calculateMatchInfo(jobStack, candidateSkills, jobEntity, memberId, true);
    }

    /**
     * 채용공고 기술 스택과 지원자 기술 스택 간 매칭 정보 계산 (기존 메서드 - 하위 호환성)
     * 
     * @param jobStack        채용공고의 기술 스택 (쉼표로 구분된 문자열)
     * @param candidateSkills 지원자의 기술 스택 Set
     * @return 매칭 정보 DTO
     */
    @Deprecated
    public JobMatchInfoDTO calculateMatchInfo(String jobStack, Set<String> candidateSkills) {
        return calculateMatchInfo(jobStack, candidateSkills, null, null, false);
    }

    /**
     * 채용공고와 지원자 간 종합 매칭 정보 계산 (내부 메서드)
     */
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

        // 채용공고 요구 스택 파싱 (쉼표, 슬래시, 공백 등으로 구분)
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

        // candidateSkills null 체크
        if (candidateSkills == null) {
            candidateSkills = Collections.emptySet();
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
                    .anyMatch(candidate -> candidate.equals(normalizedRequired) ||
                            candidate.contains(normalizedRequired) ||
                            normalizedRequired.contains(candidate) ||
                            isSynonymMatch(normalizedRequired, candidate));

            if (matched) {
                matchedStacks.add(required);
            } else {
                missingStacks.add(required);
            }
        }

        // 기술 스택 매칭률 계산
        int stackMatchRate = requiredStacks.isEmpty() ? 0
                : (int) Math.round((double) matchedStacks.size() / requiredStacks.size() * 100);

        // 벡터 유사도 매칭률 계산
        int vectorMatchRate = 0;
        if (includeVectorMatch && jobEntity != null && memberId != null) {
            try {
                vectorMatchRate = calculateVectorSimilarity(jobEntity, memberId);
            } catch (Exception e) {
                log.warn("벡터 유사도 계산 중 오류 발생: {}", e.getMessage());
                vectorMatchRate = 0;
            }
        }

        // 종합 매칭률 계산 (기술 스택 40% + 벡터 유사도 60%)
        int overallMatchRate;
        if (includeVectorMatch && vectorMatchRate > 0) {
            overallMatchRate = (int) Math.round(stackMatchRate * 0.4 + vectorMatchRate * 0.6);
        } else {
            // 원본 로직: 벡터 매칭이 없으면 기술 스택 100%
            overallMatchRate = stackMatchRate;
        }

        String matchLevel = JobMatchInfoDTO.calculateMatchLevel(overallMatchRate);

        log.debug("매칭 계산 - 요구: {}, 보유: {}, 일치: {}, 스택매칭: {}%, 벡터매칭: {}%, 종합: {}%",
                requiredStacks, candidateSkills, matchedStacks, stackMatchRate, vectorMatchRate, overallMatchRate);

        JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                .stackMatchRate(stackMatchRate)
                .vectorMatchRate(vectorMatchRate)
                .requiredStacks(requiredStacks)
                .matchedStacks(matchedStacks)
                .missingStacks(missingStacks)
                .matchLevel(matchLevel)
                .build();

        // 종합 매칭률 설정 (자동으로 matchRate와 matchLevel 업데이트)
        matchInfo.setOverallMatchRate(overallMatchRate);

        return matchInfo;
    }

    /**
     * [AI 매칭 핵심 로직] 숙련도 및 경력을 기반으로 채용공고-지원자 매칭 정보를 계산합니다.
     */
    public JobMatchInfoDTO calculateMatchInfoWithProficiency(String jobStack, Set<String> candidateSkills,
            Map<String, Integer> proficiencyMap,
            JobEntity jobEntity, Long memberId,
            boolean includeVectorMatch, List<Double> preFetchedResumeEmbedding,
            Integer preFetchedCandidateExperience) {

        if (jobStack == null || jobStack.isBlank()) {
            return createEmptyMatchInfo();
        }

        List<String> requiredStacks = parseStackString(jobStack);
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
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        List<String> matchedStacks = new ArrayList<>();
        List<String> missingStacks = new ArrayList<>();
        List<JobMatchInfoDTO.SkillMatchDetail> skillDetails = new ArrayList<>();

        for (String required : requiredStacks) {
            String normalizedRequired = required.toLowerCase().trim();
            boolean matched = normalizedCandidateSkills.stream()
                    .anyMatch(candidate -> candidate.equals(normalizedRequired) ||
                            candidate.contains(normalizedRequired) ||
                            normalizedRequired.contains(candidate) ||
                            isSynonymMatch(normalizedRequired, candidate));

            int proficiency = 0;
            if (matched) {
                matchedStacks.add(required);
                proficiency = proficiencyMap.getOrDefault(normalizedRequired, 1);
                if (proficiency == 0) {
                    for (String candidate : normalizedCandidateSkills) {
                        if (isSynonymMatch(normalizedRequired, candidate)) {
                            proficiency = proficiencyMap.getOrDefault(candidate, 1);
                            if (proficiency > 0)
                                break;
                        }
                    }
                }
                if (proficiency == 0)
                    proficiency = 1;
            } else {
                missingStacks.add(required);
            }
            skillDetails.add(JobMatchInfoDTO.SkillMatchDetail.of(required, matched, proficiency));
        }

        int stackMatchRate = (int) Math.round((matchedStacks.size() / (double) requiredStacks.size()) * 100);

        int vectorMatchRate = 0;
        if (includeVectorMatch && jobEntity != null && (memberId != null || preFetchedResumeEmbedding != null)) {
            try {
                if (preFetchedResumeEmbedding != null && jobEntity.getEmbedding() != null) {
                    double cosineSimilarity = calculateCosineSimilarity(jobEntity.getEmbedding(),
                            preFetchedResumeEmbedding);
                    vectorMatchRate = (int) Math.round(Math.max(0, Math.min(1, cosineSimilarity)) * 100);
                } else if (memberId != null) {
                    vectorMatchRate = calculateVectorSimilarity(jobEntity, memberId);
                }
            } catch (Exception e) {
                log.warn("벡터 유사도 계산 중 오류 발생: {}", e.getMessage());
            }
        }

        int experienceMatchRate = 100;
        Integer requiredExperience = null;
        Integer candidateExperience = null;

        if (jobEntity != null && (memberId != null || preFetchedCandidateExperience != null)) {
            requiredExperience = jobEntity.getRequiredExperience();
            candidateExperience = (preFetchedCandidateExperience != null) ? preFetchedCandidateExperience
                    : getCandidateExperience(memberId);

            if (requiredExperience != null && requiredExperience > 0) {
                if (candidateExperience != null && candidateExperience >= requiredExperience) {
                    experienceMatchRate = 100;
                } else if (candidateExperience != null) {
                    experienceMatchRate = (int) Math.round((candidateExperience / (double) requiredExperience) * 100);
                    experienceMatchRate = Math.min(experienceMatchRate, 100);
                } else {
                    experienceMatchRate = 0;
                }
            }
        }

        int overallMatchRate;
        if (includeVectorMatch && vectorMatchRate > 0) {
            overallMatchRate = (int) Math
                    .round(stackMatchRate * 0.5 + vectorMatchRate * 0.3 + experienceMatchRate * 0.2);
        } else {
            overallMatchRate = (int) Math.round(stackMatchRate * 0.7 + experienceMatchRate * 0.3);
        }

        JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                .stackMatchRate(stackMatchRate).vectorMatchRate(vectorMatchRate)
                .experienceMatchRate(experienceMatchRate)
                .requiredExperience(requiredExperience).candidateExperience(candidateExperience)
                .requiredStacks(requiredStacks).matchedStacks(matchedStacks).missingStacks(missingStacks)
                .matchLevel(JobMatchInfoDTO.calculateMatchLevel(overallMatchRate))
                .skillDetails(skillDetails).candidateStacks(new ArrayList<>(candidateSkills))
                .skillProficiencyMap(proficiencyMap).build();

        matchInfo.setOverallMatchRate(overallMatchRate);
        return matchInfo;
    }

    private JobMatchInfoDTO createEmptyMatchInfo() {
        JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                .stackMatchRate(0).vectorMatchRate(0).requiredStacks(Collections.emptyList())
                .matchedStacks(Collections.emptyList()).missingStacks(Collections.emptyList())
                .matchLevel("LOW").build();
        matchInfo.setOverallMatchRate(0);
        return matchInfo;
    }

    private Integer getCandidateExperience(Long memberId) {
        try {
            Optional<Resume> resumeOpt = resumeRepository.findByUser_IdAndPrimaryTrue(memberId);
            if (resumeOpt.isEmpty()) {
                resumeOpt = resumeRepository.findFirstByUser_IdOrderByLastModifiedAtDesc(memberId);
            }
            if (resumeOpt.isPresent()) {
                Integer careerYears = resumeOpt.get().getCareerYears();
                return careerYears != null ? careerYears : 0;
            }
        } catch (Exception e) {
            log.warn("지원자 경력 조회 실패 (memberId: {}): {}", memberId, e.getMessage());
        }
        return null;
    }

    public int calculateVectorSimilarity(JobEntity jobEntity, Long memberId) {
        try {
            if (memberId == null || jobEntity == null)
                return 0;
            List<Double> jobEmbedding = jobEntity.getEmbedding();
            if (jobEmbedding == null || jobEmbedding.isEmpty())
                return 0;
            Optional<Resume> resumeOpt = resumeRepository.findByUser_IdAndPrimaryTrue(memberId);
            if (resumeOpt.isEmpty())
                return 0;
            List<Double> resumeEmbedding = resumeOpt.get().getEmbedding();
            if (resumeEmbedding == null || resumeEmbedding.isEmpty())
                return 0;
            double cosineSimilarity = calculateCosineSimilarity(jobEmbedding, resumeEmbedding);
            return (int) Math.round(Math.max(0, Math.min(1, cosineSimilarity)) * 100);
        } catch (Exception e) {
            log.warn("벡터 유사도 계산 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }

    public double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1.size() != vector2.size())
            throw new IllegalArgumentException("벡터 차원이 일치하지 않습니다.");
        double dotProduct = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < vector1.size(); i++) {
            double v1 = vector1.get(i), v2 = vector2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0.0 ? 0.0 : dotProduct / denominator;
    }

    public List<String> parseStackString(String stackString) {
        if (stackString == null || stackString.isBlank())
            return Collections.emptyList();
        String cleaned = stackString.replaceAll("^\\{|\\}$", "");
        return Arrays.stream(cleaned.split("[,/;|]")).map(String::trim)
                .map(s -> s.replaceAll("^\"|\"$", "")).map(String::trim)
                .filter(s -> !s.isEmpty() && !s.isBlank()).collect(Collectors.toList());
    }

    public boolean isSynonymMatch(String stack1, String stack2) {
        Map<String, Set<String>> synonyms = Map.ofEntries(
                Map.entry("javascript", Set.of("js", "자바스크립트")),
                Map.entry("typescript", Set.of("ts", "타입스크립트")),
                Map.entry("react", Set.of("reactjs", "react.js", "리액트")),
                Map.entry("vue", Set.of("vuejs", "vue.js", "뷰")),
                Map.entry("angular", Set.of("angularjs", "angular.js", "앵귤러")),
                Map.entry("node", Set.of("nodejs", "node.js", "노드")),
                Map.entry("spring", Set.of("springboot", "spring boot", "스프링")),
                Map.entry("java", Set.of("자바")), Map.entry("python", Set.of("파이썬")),
                Map.entry("kotlin", Set.of("코틀린")), Map.entry("postgresql", Set.of("postgres", "포스트그레스")),
                Map.entry("mysql", Set.of("마이에스큐엘")), Map.entry("mongodb", Set.of("mongo", "몽고디비")),
                Map.entry("aws", Set.of("amazon web services", "아마존")),
                Map.entry("gcp", Set.of("google cloud", "구글클라우드")),
                Map.entry("docker", Set.of("도커")), Map.entry("kubernetes", Set.of("k8s", "쿠버네티스")));
        for (Map.Entry<String, Set<String>> entry : synonyms.entrySet()) {
            Set<String> allVariants = new HashSet<>(entry.getValue());
            allVariants.add(entry.getKey());
            if (allVariants.contains(stack1) && allVariants.contains(stack2))
                return true;
        }
        return false;
    }
}
