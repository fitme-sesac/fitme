package com.example.pproject.talent.service;

import com.example.pproject.Constant.ResumeStatus;
import com.example.pproject.resume.entity.Resume;
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
import java.util.List;
import java.util.Optional;

/**
 * 인재풀 서비스 - DB의 구직자(이력서) 데이터 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TalentPoolService {

    private final ResumeRepository resumeRepository;

    /**
     * 인재풀 목록 조회 (페이지네이션)
     * - CANDIDATE 역할의 회원 중 ACTIVE 이력서가 있는 구직자
     * - 회원당 대표 이력서 1건만 노출
     */
    @Transactional(readOnly = true)
    public Page<TalentListItemDTO> getTalentPool(Pageable pageable) {
        Page<Resume> resumePage = resumeRepository.findActiveResumesForTalentPool(pageable);
        List<TalentListItemDTO> list = resumePage.getContent().stream()
                .map(this::toListItemDTO)
                .toList();
        return new PageImpl<>(list, pageable, resumePage.getTotalElements());
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

    private TalentListItemDTO toListItemDTO(Resume r) {
        UserEntity u = r.getUser();
        String experience = formatExperience(r.getCareerYears());
        String lastUpdated = formatLastUpdated(r.getLastModifiedAt());
        boolean isNew = isRecentlyUpdated(r.getLastModifiedAt(), 7);

        return TalentListItemDTO.builder()
                .id(u.getId())
                .resumeId(r.getId())
                .name(u.getUsername())
                .title(r.getTagline() != null && !r.getTagline().isBlank() ? r.getTagline() : r.getTitle())
                .summary(r.getSummary())
                .experience(experience)
                .location(r.getPreferenceLocation() != null ? r.getPreferenceLocation() : "-")
                .education(r.getSchool() != null ? r.getSchool() : "-")
                .skills(r.getReStack() != null ? r.getReStack() : List.of())
                .salary(r.getPreferenceSalary() != null ? r.getPreferenceSalary() : "협의")
                .matchScore(0) // 추후 매칭 점수 적용
                .lastUpdated(lastUpdated)
                .isNew(isNew)
                .build();
    }

    private TalentDetailDTO toDetailDTO(Resume r, boolean isCompany) {
        UserEntity u = r.getUser();
        String experience = formatExperience(r.getCareerYears());
        String lastUpdated = formatLastUpdated(r.getLastModifiedAt());
        boolean isNew = isRecentlyUpdated(r.getLastModifiedAt(), 7);

        String email = isCompany ? u.getEmail() : null;
        String phone = isCompany && u.getPhone() != null
                ? maskPhone(u.getPhone()) : null;

        return TalentDetailDTO.builder()
                .id(u.getId())
                .name(u.getUsername())
                .title(r.getTagline() != null && !r.getTagline().isBlank() ? r.getTagline() : r.getTitle())
                .summary(r.getSummary())
                .experience(experience)
                .location(r.getPreferenceLocation() != null ? r.getPreferenceLocation() : "-")
                .education(r.getSchool() != null ? r.getSchool() : "-")
                .skills(r.getReStack() != null ? r.getReStack() : List.of())
                .salary(r.getPreferenceSalary() != null ? r.getPreferenceSalary() : "협의")
                .matchScore(0)
                .lastUpdated(lastUpdated)
                .isNew(isNew)
                .email(email)
                .phone(phone)
                .build();
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
