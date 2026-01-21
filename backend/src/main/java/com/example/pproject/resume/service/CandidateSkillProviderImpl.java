package com.example.pproject.resume.service;

import com.example.pproject.job.service.CandidateSkillProvider;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.entity.ResumeProject;
import com.example.pproject.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 지원자(Candidate)의 기술 스택 정보를 제공하는 구현체
 * 
 * - 이력서의 re_stack 필드에서 보유 기술 스택 추출
 * - 각 프로젝트의 tech_stack 필드에서 프로젝트 기술 스택 추출
 * - 모두 통합하여 중복 제거된 Set으로 반환
 * 
 * Note: 트랜잭션을 REQUIRES_NEW로 설정하여 상위 트랜잭션에 영향을 주지 않도록 함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateSkillProviderImpl implements CandidateSkillProvider {
    
    private final ResumeRepository resumeRepository;
    
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public Set<String> getSkillsByMemberId(Long memberId) {
        if (memberId == null) {
            return new HashSet<>();
        }
        
        try {
            // 대표(primary) 이력서 기준으로 조회
            // UserEntity의 id는 Integer 타입이므로 변환
            Optional<Resume> resumeOpt = resumeRepository.findByUserIdAndPrimaryTrue(memberId.intValue());
            
            if (resumeOpt.isEmpty()) {
                log.debug("회원 {}의 대표 이력서가 없습니다.", memberId);
                return new HashSet<>();
            }
            
            Resume resume = resumeOpt.get();
            return extractSkillsFromResume(resume);
        } catch (Exception e) {
            log.warn("회원 {}의 기술 스택 조회 중 오류: {}", memberId, e.getMessage());
            return new HashSet<>();
        }
    }
    
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public Set<String> getSkillsByResumeId(Long resumeId) {
        if (resumeId == null) {
            return new HashSet<>();
        }
        
        try {
            return resumeRepository.findById(resumeId)
                    .map(this::extractSkillsFromResume)
                    .orElse(new HashSet<>());
        } catch (Exception e) {
            log.warn("이력서 {}의 기술 스택 조회 중 오류: {}", resumeId, e.getMessage());
            return new HashSet<>();
        }
    }
    
    /**
     * 이력서에서 기술 스택 추출
     * - resume.re_stack: 보유 기술 스택
     * - resume_project.tech_stack: 프로젝트별 사용 기술
     */
    private Set<String> extractSkillsFromResume(Resume resume) {
        Set<String> skills = new HashSet<>();
        
        try {
            // 1. re_stack 파싱 (기본 기술 스택)
            if (resume.getReStack() != null && !resume.getReStack().isBlank()) {
                parseAndAddSkills(resume.getReStack(), skills);
            }
            
            // 2. 각 프로젝트의 tech_stack 파싱
            // Lazy Loading 문제 방지를 위해 try-catch로 감싸기
            try {
                if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
                    for (ResumeProject project : resume.getProjects()) {
                        if (project.getTechStack() != null && !project.getTechStack().isBlank()) {
                            parseAndAddSkills(project.getTechStack(), skills);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("프로젝트 기술 스택 로딩 중 오류 (무시됨): {}", e.getMessage());
            }
            
            log.debug("이력서 {}에서 {} 개의 기술 스택 추출: {}", 
                    resume.getId(), skills.size(), skills);
        } catch (Exception e) {
            log.warn("이력서 {}에서 기술 스택 추출 중 오류: {}", resume.getId(), e.getMessage());
        }
        
        return skills;
    }
    
    /**
     * 스택 문자열 파싱 후 Set에 추가
     * - 구분자: 쉼표(,), 슬래시(/), 세미콜론(;), 파이프(|)
     * - 소문자로 정규화
     */
    private void parseAndAddSkills(String stackString, Set<String> skills) {
        if (stackString == null || stackString.isBlank()) {
            return;
        }
        
        Arrays.stream(stackString.split("[,/;|]"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .forEach(skills::add);
    }
}
