package com.example.pproject.resume.service;

import com.example.pproject.job.service.CandidateSkillProvider;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.entity.ResumeProject;
import com.example.pproject.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 이력서 기술 스택 서비스
 * - CandidateSkillProvider 구현으로 Job 매칭에 활용
 * - re_stack과 tech_stack(프로젝트)을 통합하여 숙련도 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeSkillService implements CandidateSkillProvider {

    private final ResumeRepository resumeRepository;

    /**
     * CandidateSkillProvider 구현 - Long memberId 버전
     */
    @Override
    public Set<String> getSkillsByMemberId(Long memberId) {
        return getSkillsByMemberId(memberId.intValue());
    }

    /**
     * 1. 특정 회원의 기술 스택 조회 (대표 이력서 기준)
     */
    public Set<String> getSkillsByMemberId(Integer memberId) {
        Optional<Resume> primaryResume = findPrimaryResume(memberId);
        return primaryResume
                .map(this::extractSkillsFromResume)
                .orElse(new HashSet<>());
    }

    /**
     * 2. 특정 이력서의 기술 스택 조회
     */
    public Set<String> getSkillsByResumeId(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .map(this::extractSkillsFromResume)
                .orElse(new HashSet<>());
    }
    
    /**
     * 3. 특정 회원의 기술 스택 + 숙련도 맵 조회 (매칭 상세 분석용)
     * - re_stack과 tech_stack에서 중복될수록 숙련도 높음 (최대 3)
     * @return Map<기술명(소문자), 숙련도(1-3)>
     */
    public Map<String, Integer> getSkillProficiencyMap(Long memberId) {
        Optional<Resume> primaryResume = findPrimaryResume(memberId.intValue());
        return primaryResume
                .map(this::extractSkillProficiencyFromResume)
                .orElse(new HashMap<>());
    }
    
    /**
     * 4. 특정 이력서의 기술 스택 + 숙련도 맵 조회
     */
    public Map<String, Integer> getSkillProficiencyMapByResumeId(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .map(this::extractSkillProficiencyFromResume)
                .orElse(new HashMap<>());
    }
    
    /**
     * 대표 이력서 찾기 (없으면 최근 수정된 이력서)
     */
    private Optional<Resume> findPrimaryResume(Integer memberId) {
        Optional<Resume> primaryResume = resumeRepository.findByUserIdAndPrimaryTrue(memberId);
        if (primaryResume.isEmpty()) {
            primaryResume = resumeRepository.findFirstByUserIdOrderByLastModifiedAtDesc(memberId);
        }
        return primaryResume;
    }
    
    /**
     * 이력서에서 기술 스택만 추출 (Set)
     */
    private Set<String> extractSkillsFromResume(Resume resume) {
        Map<String, Integer> proficiencyMap = extractSkillProficiencyFromResume(resume);
        return new HashSet<>(proficiencyMap.keySet());
    }
    
    /**
     * 이력서에서 기술 스택 + 숙련도 추출
     * - re_stack: 기본 1점
     * - tech_stack(프로젝트): 프로젝트 수만큼 추가 (+1, 최대 3)
     * - 중복 시 가산 (re_stack + tech_stack 모두 있으면 더 높은 점수)
     */
    private Map<String, Integer> extractSkillProficiencyFromResume(Resume resume) {
        Map<String, Integer> skillCountMap = new HashMap<>();
        
        // 1) re_stack에서 추출 (기본 1점)
        List<String> reStack = resume.getReStack();
        if (reStack != null) {
            for (String skill : reStack) {
                if (skill != null && !skill.isBlank()) {
                    String normalized = normalizeSkill(skill);
                    skillCountMap.merge(normalized, 1, Integer::sum);
                }
            }
        }
        
        // 2) 프로젝트의 tech_stack에서 추출 (각 프로젝트당 1점 추가)
        List<ResumeProject> projects = resume.getProjects();
        if (projects != null) {
            for (ResumeProject project : projects) {
                String techStack = project.getTechStack();
                if (techStack != null && !techStack.isBlank()) {
                    // tech_stack은 콤마로 구분된 문자열
                    List<String> techs = parseStackString(techStack);
                    for (String tech : techs) {
                        String normalized = normalizeSkill(tech);
                        if (!normalized.isEmpty()) {
                            skillCountMap.merge(normalized, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        
        // 3) 숙련도로 변환 (카운트 → 1~3 범위)
        Map<String, Integer> proficiencyMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : skillCountMap.entrySet()) {
            // 카운트가 1이면 1(초급), 2이면 2(중급), 3이상이면 3(고급)
            int proficiency = Math.min(3, entry.getValue());
            proficiencyMap.put(entry.getKey(), proficiency);
        }
        
        log.debug("기술 스택 숙련도 분석: {}", proficiencyMap);
        return proficiencyMap;
    }
    
    /**
     * 기술명 정규화 (소문자, 트림)
     */
    private String normalizeSkill(String skill) {
        if (skill == null) return "";
        return skill.trim().toLowerCase();
    }
    
    /**
     * 스택 문자열 파싱 (콤마, 슬래시, 세미콜론 등으로 구분)
     */
    private List<String> parseStackString(String stackStr) {
        if (stackStr == null || stackStr.isBlank()) {
            return Collections.emptyList();
        }
        
        // 중괄호 제거
        stackStr = stackStr.replaceAll("[{}]", "");
        
        // 다양한 구분자로 분리
        return Arrays.stream(stackStr.split("[,;/|\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}