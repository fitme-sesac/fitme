package com.example.pproject.resume.service;

import com.example.pproject.common.util.IndustryUtil;
import com.example.pproject.common.util.JobPositionUtil;
import com.example.pproject.job.dto.JobDTO;
import com.example.pproject.job.dto.JobListResponseDTO;
import com.example.pproject.job.dto.JobMatchInfoDTO;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobEntityRepository;
import com.example.pproject.job.service.JobService;
import com.example.pproject.resume.dto.JobRecommendationDTO;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 이력서-채용공고 매칭을 담당하는 핵심 서비스입니다.
 *
 * <아키텍처 전략: 2-Phase Matching>
 * 1. Recall Phase (DB): JobEntityRepository.findPublicJobsWithSimilarity
 * - HNSW 인덱스를 활용하여 수십만 건의 공고 중 유사도가 높은 상위 후보군(1000건)을 고속 추출합니다.
 * 2. Calculation Phase (Application): calculateRefinedMatchInfo
 * - 추출된 후보군에 대해 기술 스택 숙련도, 상세 경력 조건 등 정밀한 비즈니스 로직(5:3:2)을 적용하여 최종 매칭 점수를 산출합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeMatchService {

    private final ResumeRepository resumeRepository;
    private final JobEntityRepository jobRepository;
    private final JobService jobService;
    private final ResumeSkillService resumeSkillService;

    private static final double DEFAULT_MIN_SIMILARITY = 0.3;

    public List<JobRecommendationDTO> recommendJobs(Long memberId, int limit, String location, List<String> skills) {
        Optional<Resume> primaryResume = resumeRepository.findByUser_IdAndPrimaryTrue(memberId);
        if (primaryResume.isEmpty() || primaryResume.get().getEmbedding() == null) {
            log.info("No primary resume or embedding for memberId: {}", memberId);
            return Collections.emptyList();
        }
        return getRecommendations(primaryResume.get(), limit, location, skills);
    }

    public List<JobRecommendationDTO> recommendJobsByResume(Long resumeId, int limit, String location, List<String> skills) {
        Optional<Resume> resume = resumeRepository.findById(resumeId);
        if (resume.isEmpty() || resume.get().getEmbedding() == null) {
            log.info("Resume not found or no embedding. ResumeId: {}", resumeId);
            return Collections.emptyList();
        }
        return getRecommendations(resume.get(), limit, location, skills);
    }

    private List<JobRecommendationDTO> getRecommendations(Resume resume, int limit, String location, List<String> skills) {
        List<Double> userEmbedding = resume.getEmbedding();
        String embeddingString = userEmbedding.toString();

        // 기존 코드가 단일 스킬만 필터링하도록 되어있어 우선 유지 (repo 쿼리 시그니처에 맞춤)
        String skillFilter = (skills == null || skills.isEmpty()) ? null : skills.get(0);

        List<Object[]> results = jobRepository.findJobsBySimilarity(
                embeddingString,
                DEFAULT_MIN_SIMILARITY,
                location,
                skillFilter,
                limit
        );

        Set<String> candidateSkills = resumeSkillService.getSkillsByMemberId(resume.getUser().getId());
        Map<String, Integer> proficiencyMap = resumeSkillService.getSkillProficiencyMap(resume.getUser().getId());
        Integer candidateExperience = resume.getCareerYears();
        List<Double> resumeEmbedding = resume.getEmbedding();

        return results.stream()
                .map(row -> {
                    JobRecommendationDTO dto = JobRecommendationDTO.fromQueryResult(row);

                    // 정밀 매칭 엔진 가동 (가중치 5:3:2 적용)
                    // row[4] = stack list (기존 로직 유지)
                    JobMatchInfoDTO matchInfo = calculateRefinedMatchInfo(
                            (String) row[4],
                            candidateSkills,
                            proficiencyMap,
                            null, // JobEntity를 넣지 못하므로 vectorMatchRate는 0으로 남음(기존 로직 동일)
                            resumeEmbedding,
                            candidateExperience
                    );
                    dto.setMatchRate(matchInfo.getOverallMatchRate());
                    return dto;
                })
                .sorted((d1, d2) -> Integer.compare(d2.getMatchRate(), d1.getMatchRate()))
                .toList();
    }

    /**
     * [AI 정밀 매칭 엔진] 숙련도, 벡터 유사도, 경력을 가중치(5:3:2)에 따라 합산합니다.
     */
    public JobMatchInfoDTO calculateRefinedMatchInfo(
            String jobStack,
            Set<String> candidateSkills,
            Map<String, Integer> proficiencyMap,
            JobEntity jobEntity,
            List<Double> resumeEmbedding,
            Integer candidateExperience
    ) {
        if (jobStack == null || jobStack.isBlank()) return createEmptyMatchInfo();

        List<String> requiredStacks = jobService.parseStackString(jobStack);
        if (requiredStacks.isEmpty()) return createEmptyMatchInfo();

        Set<String> normalizedSkills = candidateSkills.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        List<String> matchedStacks = new ArrayList<>();
        List<String> missingStacks = new ArrayList<>();
        List<JobMatchInfoDTO.SkillMatchDetail> skillDetails = new ArrayList<>();

        for (String required : requiredStacks) {
            String reqLower = required.toLowerCase().trim();

            boolean matched = normalizedSkills.stream()
                    .anyMatch(c ->
                            c.equals(reqLower) ||
                                    c.contains(reqLower) ||
                                    reqLower.contains(c) ||
                                    jobService.isSynonymMatch(reqLower, c)
                    );

            int proficiency = 0;
            if (matched) {
                matchedStacks.add(required);
                proficiency = proficiencyMap.getOrDefault(reqLower, 1);
                if (proficiency == 0) proficiency = 1;
            } else {
                missingStacks.add(required);
            }

            skillDetails.add(JobMatchInfoDTO.SkillMatchDetail.of(required, matched, proficiency));
        }

        int stackMatchRate = (int) Math.round((matchedStacks.size() / (double) requiredStacks.size()) * 100);

        int vectorMatchRate = 0;
        if (resumeEmbedding != null && jobEntity != null && jobEntity.getEmbedding() != null) {
            double similarity = jobService.calculateCosineSimilarity(jobEntity.getEmbedding(), resumeEmbedding);
            vectorMatchRate = (int) Math.round(Math.max(0, Math.min(1, similarity)) * 100);
        }

        int expMatchRate = 100;
        Integer reqExp = (jobEntity != null) ? jobEntity.getRequiredExperience() : 0;
        if (reqExp != null && reqExp > 0) {
            if (candidateExperience != null && candidateExperience >= reqExp) expMatchRate = 100;
            else if (candidateExperience != null) expMatchRate = (int) Math.round((candidateExperience / (double) reqExp) * 100);
            else expMatchRate = 0;
        }

        int overallRate = (int) Math.round(stackMatchRate * 0.5 + vectorMatchRate * 0.3 + expMatchRate * 0.2);

        JobMatchInfoDTO matchInfo = JobMatchInfoDTO.builder()
                .stackMatchRate(stackMatchRate)
                .vectorMatchRate(vectorMatchRate)
                .experienceMatchRate(expMatchRate)
                .requiredExperience(reqExp)
                .candidateExperience(candidateExperience)
                .requiredStacks(requiredStacks)
                .matchedStacks(matchedStacks)
                .missingStacks(missingStacks)
                .matchLevel(JobMatchInfoDTO.calculateMatchLevel(overallRate))
                .skillDetails(skillDetails)
                .candidateStacks(new ArrayList<>(candidateSkills))
                .skillProficiencyMap(proficiencyMap)
                .build();

        matchInfo.setOverallMatchRate(overallRate);
        return matchInfo;
    }

    private JobMatchInfoDTO createEmptyMatchInfo() {
        JobMatchInfoDTO info = JobMatchInfoDTO.builder()
                .stackMatchRate(0)
                .vectorMatchRate(0)
                .matchLevel("LOW")
                .build();
        info.setOverallMatchRate(0);
        return info;
    }

    /**
     * [고성능 매칭 검색] 지원자의 이력서 벡터를 기반으로 필터링된 채용공고 목록을 검색하고 매칭 정보를 계산합니다.
     *
     * @param memberId 지원자 회원 ID
     * @return 필터링 및 매칭 정보가 포함된 공고 목록 응답 DTO
     */
    public JobListResponseDTO getPublicJobsWithMatch(
            int page,
            int size,
            String keyword,
            String stack,
            String location,
            Integer minExperience,
            Integer maxExperience,
            String position,
            String industry,
            Long memberId
    ) {
        Optional<Resume> resumeOpt = resumeRepository.findByUser_IdAndPrimaryTrue(memberId);

        // ✅ 임베딩이 없으면: 기존 JobService의 "존재하는 시그니처"로 fallback
        if (resumeOpt.isEmpty() || resumeOpt.get().getEmbedding() == null) {
            log.debug("사용자 {}의 대표 이력서 또는 벡터가 없습니다. 기본 검색 결과로 반환합니다.", memberId);

            // position/industry는 JobService 시그니처가 없어서 keyword에 합쳐 최대한 유지
            String mergedKeyword = mergeKeywordForFallback(keyword, position, industry);

            // ⚠️ 여기 라인은 당신 JobService에 실제로 존재하는 getPublicJobs 시그니처여야 합니다.
            // 보통 (page,size,keyword,stack,location,minExp,maxExp) 형태가 있어서 그걸로 맞췄습니다.
            return jobService.getPublicJobs(page, size, mergedKeyword, stack, location, minExperience, maxExperience);
        }

        Resume resume = resumeOpt.get();
        String userEmbeddingStr = resume.getEmbedding().toString();
        Integer candidateExperience = resume.getCareerYears();

        String searchKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String searchStack = (stack != null && !stack.isBlank()) ? stack.trim() : null;

        String positionKeywords = null;
        if (position != null && !position.isBlank() && !"전체".equals(position.trim())) {
            Set<String> keywords = JobPositionUtil.getKeywordsByPositions(position);
            if (!keywords.isEmpty()) {
                positionKeywords = keywords.stream()
                        .map(kw -> "%" + kw.toLowerCase() + "%")
                        .collect(Collectors.joining(","));
            }
        }

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
            if (!allKeywords.isEmpty()) industryKeywords = String.join(",", allKeywords);
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<JobEntity> jobPage = jobRepository.findPublicJobsWithSimilarity(
                userEmbeddingStr,
                DEFAULT_MIN_SIMILARITY,
                searchKeyword,
                searchStack,
                location,
                minExperience,
                maxExperience,
                positionKeywords,
                industryKeywords,
                pageRequest
        );

        Set<String> candidateSkills = resumeSkillService.getSkillsByMemberId(memberId);
        Map<String, Integer> proficiencyMap = resumeSkillService.getSkillProficiencyMap(memberId);
        List<Double> resumeEmbedding = resume.getEmbedding();

        List<JobDTO> jobsWithMatch = jobPage.getContent().stream()
                .map(job -> {
                    JobDTO dto = jobService.toPublicJobDTO(job);
                    try {
                        JobMatchInfoDTO matchInfo = calculateRefinedMatchInfo(
                                dto.getStack(),
                                candidateSkills,
                                proficiencyMap,
                                job,
                                resumeEmbedding,
                                candidateExperience
                        );
                        dto.setMatchInfo(matchInfo);
                    } catch (Exception e) {
                        log.debug("공고 {} 매칭 계산 실패: {}", dto.getJobId(), e.getMessage());
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        // DB는 벡터 유사도 순 → 애플리케이션 매칭률 순으로 재정렬
        jobsWithMatch.sort((j1, j2) -> {
            int rate1 = (j1.getMatchInfo() != null) ? j1.getMatchInfo().getOverallMatchRate() : 0;
            int rate2 = (j2.getMatchInfo() != null) ? j2.getMatchInfo().getOverallMatchRate() : 0;
            return Integer.compare(rate2, rate1);
        });

        return JobListResponseDTO.builder()
                .jobs(jobsWithMatch)
                .page(page)
                .size(size)
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .build();
    }

    /**
     * 임베딩이 없을 때(JobService fallback) position/industry를 keyword에 섞어서라도 검색 힌트를 주기 위한 보조 함수.
     * - JobService에 position/industry 파라미터 시그니처가 없어서 컴파일을 살리되, 기능 저하를 최소화.
     */
    private String mergeKeywordForFallback(String keyword, String position, String industry) {
        List<String> tokens = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) tokens.add(keyword.trim());
        if (position != null && !position.isBlank() && !"전체".equals(position.trim())) tokens.add(position.trim());
        if (industry != null && !industry.isBlank() && !"전체".equals(industry.trim())) tokens.add(industry.trim());
        if (tokens.isEmpty()) return null;
        return String.join(" ", tokens);
    }

    /**
     * [상세 페이지 매칭 정보] 특정 채용공고 상세 조회 시 매칭 정보를 함께 계산하여 반환합니다.
     */
    public JobDTO getPublicJobWithMatch(Long jobId, Long memberId) {
        JobDTO job = jobService.getPublicJob(jobId);
        if (memberId == null) return job;

        try {
            Optional<Resume> resumeOpt = resumeRepository.findByUser_IdAndPrimaryTrue(memberId);
            if (resumeOpt.isPresent()) {
                Resume resume = resumeOpt.get();
                JobEntity jobEntity = jobRepository.findById(jobId).orElse(null);

                Set<String> candidateSkills = resumeSkillService.getSkillsByMemberId(memberId);
                Map<String, Integer> proficiencyMap = resumeSkillService.getSkillProficiencyMap(memberId);

                JobMatchInfoDTO matchInfo = calculateRefinedMatchInfo(
                        job.getStack(),
                        candidateSkills,
                        proficiencyMap,
                        jobEntity,
                        resume.getEmbedding(),
                        resume.getCareerYears()
                );
                job.setMatchInfo(matchInfo);
            }
        } catch (Exception e) {
            log.warn("상세 페이지 매칭 정보 계산 실패: {}", e.getMessage());
        }
        return job;
    }
}
