# Resume 모듈 통합 가이드 - 기술 스택 매칭 기능

> **담당자**: Resume 모듈 담당자  
> **작성일**: 2026-01-21  
> **목적**: 채용공고와 지원자 이력서 간 기술 스택 매칭 기능 구현

---

## 📌 개요

지원자가 채용공고를 조회할 때 **본인의 기술 스택과 채용공고 요구 스택 간의 매칭률**을 표시하는 기능입니다.

- 채용공고 카드: 매칭률 뱃지 표시 (예: "75% 일치")
- 채용공고 상세페이지: 매칭/부족 스택 상세 표시

---

## 🔧 구현해야 할 인터페이스

### 위치
```
com.example.pproject.job.service.CandidateSkillProvider
```

### 인터페이스 정의
```java
public interface CandidateSkillProvider {
    
    /**
     * 특정 회원의 통합 기술 스택 목록을 반환합니다.
     * 
     * @param memberId 회원 ID (member 테이블의 member_id)
     * @return 중복 제거된 기술 스택 Set (소문자로 정규화된 상태)
     */
    Set<String> getSkillsByMemberId(Long memberId);
    
    /**
     * 특정 이력서의 기술 스택 목록을 반환합니다.
     * 
     * @param resumeId 이력서 ID (resume 테이블의 resume_id)
     * @return 중복 제거된 기술 스택 Set (소문자로 정규화된 상태)
     */
    Set<String> getSkillsByResumeId(Long resumeId);
}
```

---

## 📝 구현 예시

### 파일 위치 (권장)
```
com.example.pproject.resume.service.CandidateSkillProviderImpl
```

### 구현 코드

```java
package com.example.pproject.resume.service;

import com.example.pproject.job.service.CandidateSkillProvider;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.entity.ResumeProject;
import com.example.pproject.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidateSkillProviderImpl implements CandidateSkillProvider {
    
    private final ResumeRepository resumeRepository;
    
    @Override
    public Set<String> getSkillsByMemberId(Long memberId) {
        // 대표(primary) 이력서 기준으로 조회
        return resumeRepository.findByUserIdAndPrimaryTrue(memberId.intValue())
                .map(this::extractSkillsFromResume)
                .orElse(new HashSet<>());
    }
    
    @Override
    public Set<String> getSkillsByResumeId(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .map(this::extractSkillsFromResume)
                .orElse(new HashSet<>());
    }
    
    /**
     * 이력서에서 기술 스택 추출
     * - resume.re_stack: 보유 기술 스택
     * - resume_project.tech_stack: 프로젝트별 사용 기술
     */
    private Set<String> extractSkillsFromResume(Resume resume) {
        Set<String> skills = new HashSet<>();
        
        // 1. re_stack 파싱
        if (resume.getReStack() != null && !resume.getReStack().isBlank()) {
            parseAndAddSkills(resume.getReStack(), skills);
        }
        
        // 2. 각 프로젝트의 tech_stack 파싱
        if (resume.getProjects() != null) {
            for (ResumeProject project : resume.getProjects()) {
                if (project.getTechStack() != null && !project.getTechStack().isBlank()) {
                    parseAndAddSkills(project.getTechStack(), skills);
                }
            }
        }
        
        return skills;
    }
    
    /**
     * 스택 문자열 파싱 후 Set에 추가
     * - 구분자: 쉼표(,), 슬래시(/), 세미콜론(;), 파이프(|)
     * - 소문자로 정규화
     */
    private void parseAndAddSkills(String stackString, Set<String> skills) {
        Arrays.stream(stackString.split("[,/;|]"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .forEach(skills::add);
    }
}
```

---

## 📊 데이터 소스 (ERD 기준)

| 테이블 | 컬럼 | 타입 | 설명 |
|--------|------|------|------|
| `resume` | `re_stack` | TEXT | 사용자가 입력한 보유 기술 스택 |
| `resume_project` | `tech_stack` | TEXT | 프로젝트에서 사용한 기술 스택 |

### 데이터 예시
```
re_stack: "Java, Spring Boot, PostgreSQL, React"
tech_stack: "Python/Django/Redis"
```

---

## ⚠️ 주의사항

### 1. 소문자 정규화 필수
```java
// ✅ 올바른 예
skills.add("java");
skills.add("spring boot");

// ❌ 잘못된 예
skills.add("Java");
skills.add("Spring Boot");
```

### 2. null 반환 금지
```java
// ✅ 올바른 예
return new HashSet<>();

// ❌ 잘못된 예
return null;
```

### 3. 다양한 구분자 처리
```java
// 쉼표, 슬래시, 세미콜론, 파이프 모두 지원
stackString.split("[,/;|]")
```

### 4. 대표 이력서 우선
- 회원에게 여러 이력서가 있을 경우 `is_primary=true`인 이력서 기준
- 대표 이력서가 없으면 빈 Set 반환 또는 가장 최근 이력서 사용 (선택)

---

## 🔄 동작 흐름

```
1. 지원자가 채용공고 목록/상세 조회 API 호출
2. PublicJobController에서 JWT로 memberId 추출
3. JobService.getPublicJobsWithMatch() 호출
4. CandidateSkillProvider.getSkillsByMemberId() 호출 ← [Resume 모듈]
5. 채용공고 stack과 비교하여 매칭률 계산
6. matchInfo 포함한 응답 반환
```

---

## 📤 API 응답 예시

```json
{
  "jobId": 1,
  "title": "백엔드 개발자",
  "stack": "Java, Spring Boot, PostgreSQL, Redis",
  "matchInfo": {
    "matchRate": 75,
    "requiredStacks": ["Java", "Spring Boot", "PostgreSQL", "Redis"],
    "matchedStacks": ["Java", "Spring Boot", "PostgreSQL"],
    "missingStacks": ["Redis"],
    "matchLevel": "GOOD"
  }
}
```

### matchLevel 기준

| Level | 매칭률 |
|-------|--------|
| `EXCELLENT` | 80% 이상 |
| `GOOD` | 60% 이상 |
| `MODERATE` | 40% 이상 |
| `LOW` | 40% 미만 |

---

## ✅ 체크리스트

- [ ] `CandidateSkillProviderImpl` 클래스 생성
- [ ] `CandidateSkillProvider` 인터페이스 구현
- [ ] `@Service` 어노테이션 추가
- [ ] `getSkillsByMemberId()` 메서드 구현
- [ ] `getSkillsByResumeId()` 메서드 구현
- [ ] 소문자 정규화 확인
- [ ] 빈 Set 반환 확인 (null 금지)
- [ ] 단위 테스트 작성

---

## 🤝 연락처

Job 모듈 관련 문의사항이 있으면 담당자에게 연락해주세요.
