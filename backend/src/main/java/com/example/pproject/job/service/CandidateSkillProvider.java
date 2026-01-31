// src/main/java/com/example/pproject/job/service/CandidateSkillProvider.java
package com.example.pproject.job.service;

import java.util.Set;

/**
 * 지원자 기술 스택 제공 인터페이스
 * - Resume 모듈에서 구현하여 Job 모듈에서 매칭 계산에 사용
 */
public interface CandidateSkillProvider {

    Set<String> getSkillsByMemberId(Long memberId);

    Set<String> getSkillsByResumeId(Long resumeId);
}
