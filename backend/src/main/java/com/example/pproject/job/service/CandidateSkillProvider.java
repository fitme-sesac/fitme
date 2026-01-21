package com.example.pproject.job.service;

import java.util.Set;

/**
 * 지원자(Candidate)의 기술 스택 정보를 제공하는 인터페이스
 * 
 * Resume 모듈에서 이 인터페이스를 구현해야 합니다.
 * Job 모듈에서는 이 인터페이스를 통해 지원자의 기술 스택을 조회합니다.
 * 
 * @see CandidateSkillProviderImpl (Resume 모듈에서 구현 필요)
 */
public interface CandidateSkillProvider {
    
    /**
     * 특정 회원의 통합 기술 스택 목록을 반환합니다.
     * 
     * 포함되어야 하는 정보:
     * 1. resume.re_stack: 이력서에 직접 입력한 보유 기술 스택
     * 2. resume_project.tech_stack: 각 프로젝트에서 사용한 기술 스택
     * 
     * @param memberId 회원 ID (member 테이블의 member_id)
     * @return 중복 제거된 기술 스택 Set (소문자로 정규화된 상태)
     *         - 이력서가 없는 경우 빈 Set 반환
     *         - 여러 이력서가 있는 경우 대표(primary) 이력서의 스택만 반환하거나
     *           모든 이력서의 스택을 통합하여 반환 (구현 방식 선택 가능)
     */
    Set<String> getSkillsByMemberId(Long memberId);
    
    /**
     * 특정 이력서의 기술 스택 목록을 반환합니다.
     * 
     * 포함되어야 하는 정보:
     * 1. resume.re_stack: 해당 이력서의 보유 기술 스택
     * 2. resume_project.tech_stack: 해당 이력서 내 모든 프로젝트의 기술 스택
     * 
     * @param resumeId 이력서 ID (resume 테이블의 resume_id)
     * @return 중복 제거된 기술 스택 Set (소문자로 정규화된 상태)
     *         - 이력서가 없는 경우 빈 Set 반환
     */
    Set<String> getSkillsByResumeId(Long resumeId);
}
