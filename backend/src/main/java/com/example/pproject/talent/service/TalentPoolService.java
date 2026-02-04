package com.example.pproject.talent.service;

import com.example.pproject.Constant.ResumeStatus;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobEntityRepository;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.entity.ResumeProject;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.talent.dto.TalentDetailDTO;
import com.example.pproject.talent.dto.TalentListItemDTO;
import com.example.pproject.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 인재풀 서비스 - DB의 구직자(이력서) 데이터 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TalentPoolService {

    private final ResumeRepository resumeRepository;
    private final JobEntityRepository jobEntityRepository;
    private final EmployerMemberRepository employerMemberRepository;

    /**
     * 인재풀 목록 조회 (페이지네이션)
     * - CANDIDATE 역할의 회원 중 ACTIVE 이력서가 있는 구직자
     * - 회원당 대표 이력서 1건만 노출
     */
    @Transactional(readOnly = true)
    public Page<TalentListItemDTO> getTalentPool(Pageable pageable) {
        return getTalentPool(pageable, null);
    }

    /**
     * 인재풀 목록 조회 (페이지네이션 + 매칭율)
     * @param pageable 페이지네이션
     * @param employerMemberId 기업회원 ID (매칭율 계산용)
     */
    @Transactional(readOnly = true)
    public Page<TalentListItemDTO> getTalentPool(Pageable pageable, Long employerMemberId) {
        // 기업의 채용공고 스택 조회 (매칭율 계산용)
        Set<String> employerRequiredStacks = getEmployerRequiredStacks(employerMemberId);
        
        // 이력서 조회
        Page<Resume> resumePage = resumeRepository.findActiveResumesForTalentPool(pageable);

        // DTO 변환 (매칭율 계산 포함)
        List<TalentListItemDTO> list = resumePage.getContent().stream()
                .map(r -> toListItemDTOWithMatchScore(r, employerRequiredStacks))
                .toList();
        
        return new PageImpl<>(list, pageable, resumePage.getTotalElements());
    }

    /**
     * 기업회원의 모든 채용공고 스택을 수집
     */
    private Set<String> getEmployerRequiredStacks(Long employerMemberId) {
        if (employerMemberId == null) {
            return Collections.emptySet();
        }

        try {
            // employer_member에서 employer_id 조회 (단일 기업만 처리)
            Optional<EmployerMemberEntity> membershipOpt = employerMemberRepository
                    .findFirstByMemberIdAndActiveTrue(employerMemberId);
            if (membershipOpt.isEmpty()) {
                return Collections.emptySet();
            }

            Long employerId = membershipOpt.get().getEmployerId();

            // 해당 기업의 모든 활성 채용공고 조회
            List<JobEntity> jobs = jobEntityRepository.findByEmployerIdAndNotDeleted(employerId);
            if (jobs.isEmpty()) {
                return Collections.emptySet();
            }

            // 모든 채용공고의 스택 수집 (중복 제거, 소문자 변환)
            Set<String> allStacks = new LinkedHashSet<>();
            for (JobEntity job : jobs) {
                if (job.getStack() != null) {
                    for (String stack : job.getStack()) {
                        String cleaned = cleanSkillString(stack);
                        if (cleaned != null && !cleaned.isEmpty()) {
                            allStacks.add(cleaned.toLowerCase());
                        }
                    }
                }
            }
            
            log.info("기업 {} 채용공고 스택 수집: {}개 공고에서 {}개 스택", 
                    employerId, jobs.size(), allStacks.size());
            return allStacks;

        } catch (Exception e) {
            log.warn("기업 스택 조회 실패 (memberId: {}): {}", employerMemberId, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 인재-기업 매칭율 계산
     * - 기업의 요구 스택 중 인재가 보유한 스택의 비율
     */
    private int calculateMatchScore(List<String> candidateSkills, Set<String> employerRequiredStacks) {
        if (employerRequiredStacks.isEmpty() || candidateSkills == null || candidateSkills.isEmpty()) {
            return 0;
        }

        Set<String> candidateSkillsLower = candidateSkills.stream()
                .map(s -> cleanSkillString(s))
                .filter(s -> s != null && !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (candidateSkillsLower.isEmpty()) {
            return 0;
        }

        // 매칭된 스택 수 계산 (유사어/동의어 매칭 포함)
        int matchedCount = 0;
        for (String required : employerRequiredStacks) {
            for (String candidate : candidateSkillsLower) {
                if (isSimilarMatch(required, candidate)) {
                    matchedCount++;
                    break; // 하나라도 매칭되면 다음 required로
                }
            }
        }

        // 매칭율 = (매칭된 스택 수 / 기업 요구 스택 수) * 100
        int matchRate = (int) Math.round((double) matchedCount / employerRequiredStacks.size() * 100);
        return Math.min(matchRate, 100); // 최대 100%
    }

    /**
     * 스택 유사어 매칭 (기본 포함 관계 + 동의어)
     */
    private boolean isSimilarMatch(String stack1, String stack2) {
        if (stack1.equals(stack2)) return true;
        if (stack1.contains(stack2) || stack2.contains(stack1)) return true;

        // 동의어 그룹
        List<Set<String>> synonymGroups = List.of(
                Set.of("javascript", "js"),
                Set.of("typescript", "ts"),
                Set.of("react", "react.js", "reactjs"),
                Set.of("vue", "vue.js", "vuejs"),
                Set.of("node", "node.js", "nodejs"),
                Set.of("next", "next.js", "nextjs"),
                Set.of("spring", "spring boot", "springboot"),
                Set.of("c#", "csharp", ".net"),
                Set.of("kubernetes", "k8s"),
                Set.of("postgresql", "postgres"),
                Set.of("react native", "reactnative"),
                Set.of("python", "py"),
                Set.of("golang", "go")
        );

        for (Set<String> group : synonymGroups) {
            if (group.contains(stack1) && group.contains(stack2)) {
                return true;
            }
        }

        return false;
    }

    private TalentListItemDTO toListItemDTOWithMatchScore(Resume r, Set<String> employerRequiredStacks) {
        UserEntity u = r.getUser();
        String experience = formatExperience(r.getCareerYears());
        String lastUpdated = formatLastUpdated(r.getLastModifiedAt());
        boolean isNew = isRecentlyUpdated(r.getLastModifiedAt(), 7);

        // re_stack + 프로젝트 tech_stack 모두 합침
        List<String> allSkills = collectAllSkills(r);

        // 매칭율 계산
        int matchScore = calculateMatchScore(allSkills, employerRequiredStacks);

        return TalentListItemDTO.builder()
                .id(u.getId())
                .resumeId(r.getId())
                .name(u.getUsername())
                .title(r.getTagline() != null && !r.getTagline().isBlank() ? r.getTagline() : r.getTitle())
                .summary(r.getSummary())
                .experience(experience)
                .location(r.getPreferenceLocation() != null ? r.getPreferenceLocation() : "-")
                .education(r.getSchool() != null ? r.getSchool() : "-")
                .skills(allSkills)
                .salary(r.getPreferenceSalary() != null ? r.getPreferenceSalary() : "협의")
                .matchScore(matchScore)
                .lastUpdated(lastUpdated)
                .isNew(isNew)
                .build();
    }

    /**
     * 인재 상세 조회 (memberId = 구직자 PK)
     */
    @Transactional(readOnly = true)
    public Optional<TalentDetailDTO> getTalentDetail(Long memberId, boolean isCompany) {
        var resumeOpt = resumeRepository.findPrimaryOrLatest(memberId);
        if (resumeOpt.isEmpty()) {
            return Optional.empty();
        }
        Resume resume = resumeOpt.get();
        if (resume.getStatus() != ResumeStatus.ACTIVE) {
            return Optional.empty();
        }
        UserEntity user = resume.getUser();
        if (user.getRoleType() != com.example.pproject.Constant.RoleType.CANDIDATE) {
            return Optional.empty();
        }

        TalentDetailDTO dto = toDetailDTO(resume, isCompany);
        return Optional.of(dto);
    }

    private TalentDetailDTO toDetailDTO(Resume r, boolean isCompany) {
        UserEntity u = r.getUser();
        String experience = formatExperience(r.getCareerYears());
        String lastUpdated = formatLastUpdated(r.getLastModifiedAt());
        boolean isNew = isRecentlyUpdated(r.getLastModifiedAt(), 7);

        String email = isCompany ? u.getEmail() : null;
        String phone = isCompany && u.getPhone() != null
                ? maskPhone(u.getPhone()) : null;

        // re_stack + 프로젝트 tech_stack 모두 합침
        List<String> allSkills = collectAllSkills(r);

        return TalentDetailDTO.builder()
                .id(u.getId())
                .name(u.getUsername())
                .title(r.getTagline() != null && !r.getTagline().isBlank() ? r.getTagline() : r.getTitle())
                .summary(r.getSummary())
                .experience(experience)
                .location(r.getPreferenceLocation() != null ? r.getPreferenceLocation() : "-")
                .education(r.getSchool() != null ? r.getSchool() : "-")
                .skills(allSkills)
                .salary(r.getPreferenceSalary() != null ? r.getPreferenceSalary() : "협의")
                .matchScore(0)
                .lastUpdated(lastUpdated)
                .isNew(isNew)
                .email(email)
                .phone(phone)
                .build();
    }

    /**
     * 이력서의 re_stack + 프로젝트별 tech_stack을 모두 수집하여 중복 제거 후 반환
     * - JSON/PostgreSQL 배열 문자 ({, }, ") 제거
     */
    private List<String> collectAllSkills(Resume r) {
        Set<String> skillSet = new LinkedHashSet<>();

        // 1. re_stack (이력서 기술 스택)
        if (r.getReStack() != null) {
            for (String skill : r.getReStack()) {
                String cleaned = cleanSkillString(skill);
                if (cleaned != null && !cleaned.isEmpty()) {
                    skillSet.add(cleaned);
                }
            }
        }

        // 2. 프로젝트별 tech_stack
        if (r.getProjects() != null) {
            for (ResumeProject project : r.getProjects()) {
                String techStack = project.getTechStack();
                if (techStack != null && !techStack.isBlank()) {
                    // { } 제거 (PostgreSQL 배열 형식인 경우)
                    String cleanedStack = techStack.trim();
                    if (cleanedStack.startsWith("{")) {
                        cleanedStack = cleanedStack.substring(1);
                    }
                    if (cleanedStack.endsWith("}")) {
                        cleanedStack = cleanedStack.substring(0, cleanedStack.length() - 1);
                    }
                    
                    // 쉼표로 구분된 tech_stack 파싱
                    Arrays.stream(cleanedStack.split(","))
                            .map(this::cleanSkillString)
                            .filter(s -> s != null && !s.isEmpty())
                            .forEach(skillSet::add);
                }
            }
        }

        return new ArrayList<>(skillSet);
    }

    /**
     * 스킬 문자열에서 JSON/PostgreSQL 배열 문자 제거
     * 예: "{\"React\"" -> "React", "\"Java\"}" -> "Java"
     */
    private String cleanSkillString(String skill) {
        if (skill == null || skill.isBlank()) {
            return null;
        }
        String cleaned = skill.trim();
        // { } " 제거
        cleaned = cleaned.replace("{", "")
                         .replace("}", "")
                         .replace("\"", "")
                         .replace("'", "")
                         .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String formatExperience(Integer years) {
        if (years == null) return "신입";
        if (years == 0) return "신입";
        if (years >= 10) return "10년+";
        return years + "년";
    }

    private String formatLastUpdated(LocalDateTime dt) {
        if (dt == null) return "-";
        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(dt.toLocalDate(), now.toLocalDate());
        if (days == 0) return "오늘";
        if (days == 1) return "1일 전";
        if (days < 7) return days + "일 전";
        if (days < 30) return (days / 7) + "주 전";
        return (days / 30) + "개월 전";
    }

    private boolean isRecentlyUpdated(LocalDateTime dt, int days) {
        if (dt == null) return false;
        return ChronoUnit.DAYS.between(dt.toLocalDate(), LocalDateTime.now().toLocalDate()) <= days;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***-****-****";
        int len = phone.length();
        if (len >= 11) {
            return phone.substring(0, 3) + "-****-" + phone.substring(7);
        }
        return "***-****-****";
    }
}
