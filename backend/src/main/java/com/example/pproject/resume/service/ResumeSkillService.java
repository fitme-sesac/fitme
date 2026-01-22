package com.example.pproject.resume.service;

import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeSkillService {

    private final ResumeRepository resumeRepository;

    /**
     * 1. 특정 회원의 기술 스택 조회 (대표 이력서 기준)
     */
    public Set<String> getSkillsByMemberId(Integer memberId) {
        // 대표 이력서 찾기
        Optional<Resume> primaryResume = resumeRepository.findByUserIdAndPrimaryTrue(memberId);

        // 없으면 최근 수정된 이력서 찾기
        if (primaryResume.isEmpty()) {
            primaryResume = resumeRepository.findFirstByUserIdOrderByLastModifiedAtDesc(memberId);
        }

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
    private Set<String> extractSkillsFromResume(Resume resume) {
        List<String> skills = resume.getReStack();

        if (skills == null || skills.isEmpty()) {
            return new HashSet<>();
        }

        return skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}