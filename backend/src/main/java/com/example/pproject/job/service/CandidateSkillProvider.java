package com.example.pproject.job.service;

import java.util.Set;

/**
 * 지원자 기술 스택 제공 인터페이스
 * - Resume 모듈에서 구현하여 Job 모듈에서 매칭 계산에 사용
 */
public interface CandidateSkillProvider {
    
    /**
     * 특정 회원의 기술 스택 조회
     * 
     * @param memberId 회원 ID
     * @return 기술 스택 Set (없으면 빈 Set)
     */
    Set<String> getSkillsByMemberId(Long memberId);
    
    /**
     * 특정 이력서의 기술 스택 조회
     * 
     * @param resumeId 이력서 ID
     * @return 기술 스택 Set (없으면 빈 Set)
     */
    Set<String> getSkillsByResumeId(Long resumeId);
}
