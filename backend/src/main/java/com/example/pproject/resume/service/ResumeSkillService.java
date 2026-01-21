package com.example.pproject.resume.service;

import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeSkillService { // implements 제거함

    private final ResumeRepository resumeRepository;

    /**
     * 1. 특정 회원의 기술 스택 조회 (대표 이력서 기준)
     * (나중에 인터페이스가 생기면 @Override 붙이세요)
     */
    public Set<String> getSkillsByMemberId(Long memberId) {
        // 대표 이력서 찾기
        Optional<Resume> primaryResume = resumeRepository.findByMemberIdAndIsPrimaryTrue(memberId);

        // 없으면 최근 수정된 이력서 찾기
        if (primaryResume.isEmpty()) {
            primaryResume = resumeRepository.findFirstByMemberIdOrderByUpdatedAtDesc(memberId);
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

    // [내부 로직] 문자열 파싱
    private Set<String> extractSkillsFromResume(Resume resume) {
        String stackString = resume.getReStack();

        if (stackString == null || stackString.isBlank()) {
            return new HashSet<>();
        }

        String[] skills = stackString.split("[,/|;]");

        return Arrays.stream(skills)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}