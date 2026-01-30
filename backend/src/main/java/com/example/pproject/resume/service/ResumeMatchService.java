package com.example.pproject.resume.service;

import com.example.pproject.job.repository.JobEntityRepository;
import com.example.pproject.resume.dto.JobRecommendationDTO;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 이력서 기반 채용공고 추천 서비스
 * - 사용자 이력서 embedding과 채용공고 embedding 간의 유사도로 추천
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeMatchService {

        private final ResumeRepository resumeRepository;
        private final JobEntityRepository jobRepository;

        private static final double DEFAULT_MIN_SIMILARITY = 0.3;

        /**
         * 이력서 기반 채용공고 추천
         * 
         * @param memberId 사용자 ID
         * @param limit    추천 개수
         * @param location 지역 필터 (선택)
         * @param skills   스택 필터 (선택)
         * @return 추천 채용공고 목록
         */
        public List<JobRecommendationDTO> recommendJobs(
                        Long memberId, int limit, String location, List<String> skills) {

                // 1. 사용자의 대표 이력서 조회
                Optional<Resume> primaryResume = resumeRepository.findByUserIdAndPrimaryTrue(memberId);

                if (primaryResume.isEmpty()) {
                        log.info("No primary resume for memberId: {}", memberId);
                        return Collections.emptyList();
                }

                if (primaryResume.get().getEmbedding() == null) {
                        log.info("No embedding for resume. ResumeId: {}", primaryResume.get().getId());
                        return Collections.emptyList();
                }

                // 2. embedding 문자열 변환
                List<Double> userEmbedding = primaryResume.get().getEmbedding();
                String embeddingString = userEmbedding.toString();

                // 3. skills 첫 번째 값만 사용 (LIKE 필터)
                String skillFilter = (skills == null || skills.isEmpty())
                                ? null
                                : skills.get(0);

                // 4. 유사도 기반 채용공고 조회
                List<Object[]> results = jobRepository.findJobsBySimilarity(
                                embeddingString, DEFAULT_MIN_SIMILARITY, location, skillFilter, limit);

                log.info("Job recommendation for memberId: {}. Found {} jobs. Location: {}, Skills: {}",
                                memberId, results.size(), location, skills);

                // 5. DTO로 변환
                return results.stream()
                                .map(JobRecommendationDTO::fromQueryResult)
                                .toList();
        }

        /**
         * 특정 이력서 기반 채용공고 추천
         */
        public List<JobRecommendationDTO> recommendJobsByResume(
                        Long resumeId, int limit, String location, List<String> skills) {

                Optional<Resume> resume = resumeRepository.findById(resumeId);

                if (resume.isEmpty() || resume.get().getEmbedding() == null) {
                        log.info("Resume not found or no embedding. ResumeId: {}", resumeId);
                        return Collections.emptyList();
                }

                List<Double> userEmbedding = resume.get().getEmbedding();
                String embeddingString = userEmbedding.toString();

                String skillFilter = (skills == null || skills.isEmpty())
                                ? null
                                : skills.get(0);

                List<Object[]> results = jobRepository.findJobsBySimilarity(
                                embeddingString, DEFAULT_MIN_SIMILARITY, location, skillFilter, limit);

                return results.stream()
                                .map(JobRecommendationDTO::fromQueryResult)
                                .toList();
        }
}
