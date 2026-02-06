package com.example.pproject.resume.service;

import com.example.pproject.application.repository.JobApplicationRepository;
import com.example.pproject.job.service.JobScrapService;
import com.example.pproject.resume.dto.ResumeRequest;
import com.example.pproject.resume.dto.ResumeResponse;
import com.example.pproject.resume.entity.*;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;

    // ✅ 내 코드 기능 유지
    private final JobScrapService jobScrapService;

    // ✅ 가져오려는 코드 기능 유지 (AI 요약/임베딩 비동기 요청)
    private final ResumeSummaryService resumeSummaryService;

    // 1. 내 이력서 목록 조회 (Long userId)
    public List<ResumeResponse> getResumes(Long userId) {
        return resumeRepository.findAllByUser_Id(userId).stream()
                .map(ResumeResponse::from)
                .collect(Collectors.toList());
    }

    // 2. 이력서 상세 조회
    public ResumeResponse getResume(Long resumeId) {
        Resume resume = getResumeEntity(resumeId);
        return ResumeResponse.from(resume);
    }

    // 3. 이력서 생성 (Long userId)
    @Transactional
    public Long createResume(ResumeRequest request, Long userId) {
        UserEntity user = getUser(userId);
        Resume resume = request.toEntity(user);

        // 첫 이력서라면 대표 이력서로 설정
        if (resumeRepository.findAllByUser_Id(userId).isEmpty()) {
            resume.setPrimary(true);
        }

        // 프로필(사진, 주소) 저장
        if (request.getProfile() != null) {
            resume.updateProfile(request.getProfile().toEntity(resume));
        }

        // 자식 엔티티 추가 (경력, 프로젝트 등)
        addChildrenToResume(resume, request);

        // ✅ 저장 먼저 해서 resumeId 확보
        Long savedId = resumeRepository.save(resume).getId();

        // ✅ (가져오려는 코드) 첫 이력서(=대표)일 때 AI 요약/임베딩 요청
        if (Boolean.TRUE.equals(resume.isPrimary())) {
            resumeSummaryService.requestSummary(savedId, userId);
        }

        return savedId;
    }

    // 4. 이력서 수정 (Long userId)
    @Transactional
    public Long updateResume(Long resumeId, ResumeRequest request, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);

        resume.updateInfo(
                request.getTitle(), request.getTagline(), request.getContent(),
                request.getPublicOption(), request.getField(),
                request.getPreferenceLocation(), request.getPreferenceSalary(),
                request.getEmploymentType(),
                request.getReStack(),     // List<String>
                request.getCareerYears(), // Integer
                request.getSchool(), request.getSchoolState(), request.getSchoolClass()
        );

        // 대표 이력서 변경 로직
        if (Boolean.TRUE.equals(request.getPrimary())) {
            resumeRepository.findByUser_IdAndPrimaryTrue(userId)
                    .ifPresent(old -> old.setPrimary(false));
            resume.setPrimary(true);

            // ✅ (가져오려는 코드) 대표 이력서로 설정/변경 시 AI 요약/임베딩 요청
            resumeSummaryService.requestSummary(resume.getId(), userId);
        }

        return resume.getId();
    }

    // 5. 이력서 삭제 (Long userId)
    @Transactional
    public void deleteResume(Long resumeId, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);

        // 입사 지원에 사용된 이력서는 삭제 불가
        if (jobApplicationRepository.existsByResumeId(resumeId)) {
            throw new IllegalStateException("이미 입사 지원에 사용된 이력서는 삭제할 수 없습니다.");
        }
        resumeRepository.delete(resume);
    }

    // 6. 이력서 복제 (Long userId)
    @Transactional
    public Long copyResume(Long resumeId, Long userId) {
        Resume original = getResumeEntity(resumeId);
        validateOwner(original, userId);

        Resume copy = Resume.builder()
                .user(original.getUser())
                .title(original.getTitle() + " (복사본)")
                .field(original.getField())
                .tagline(original.getTagline())
                .content(original.getContent())
                .preferenceLocation(original.getPreferenceLocation())
                .preferenceSalary(original.getPreferenceSalary())
                .employmentType(original.getEmploymentType())
                .reStack(original.getReStack())
                .school(original.getSchool())
                .schoolState(original.getSchoolState())
                .schoolClass(original.getSchoolClass())
                .primary(false)
                .publicOption(false)
                .build();

        if (original.getProfile() != null) {
            copy.updateProfile(ResumeProfile.builder()
                    .resume(copy)
                    .address(original.getProfile().getAddress())
                    .photoUrl(original.getProfile().getPhotoUrl())
                    .build());
        }

        return resumeRepository.save(copy).getId();
    }

    // --- 하위 항목(경력, 프로젝트 등) 관리 메소드 ---

    @Transactional
    public void addCareer(Long resumeId, ResumeRequest.CareerDto dto, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getCareers().add(dto.toEntity(resume));
    }

    @Transactional
    public void deleteCareer(Long resumeId, Long careerId, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getCareers().removeIf(career -> career.getId().equals(careerId));
    }

    @Transactional
    public void addProject(Long resumeId, ResumeRequest.ProjectDto dto, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getProjects().add(dto.toEntity(resume));
    }

    @Transactional
    public void deleteProject(Long resumeId, Long projectId, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getProjects().removeIf(project -> project.getId().equals(projectId));
    }

    @Transactional
    public void addCertificate(Long resumeId, ResumeRequest.CertificateDto dto, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getCertificates().add(dto.toEntity(resume));
    }

    @Transactional
    public void deleteCertificate(Long resumeId, Long certId, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getCertificates().removeIf(certificate -> certificate.getId().equals(certId));
    }

    @Transactional
    public void addLink(Long resumeId, ResumeRequest.LinkDto dto, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getLinks().add(dto.toEntity(resume));
    }

    @Transactional
    public void deleteLink(Long resumeId, Long linkId, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getLinks().removeIf(link -> link.getId().equals(linkId));
    }

    @Transactional
    public void updateProfileImage(Long resumeId, String photoUrl, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        if (resume.getProfile() == null) {
            resume.updateProfile(ResumeProfile.builder().resume(resume).build());
        }
        resume.getProfile().updatePhotoUrl(photoUrl);
    }

    @Transactional
    public void addAttachment(Long resumeId, ResumeRequest.AttachmentDto dto, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getAttachments().add(dto.toEntity(resume));
    }

    @Transactional
    public void deleteAttachment(Long resumeId, Long attachmentId, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getAttachments().removeIf(attachment -> attachment.getId().equals(attachmentId));
    }

    /**
     * ✅ (가져오려는 코드) 특정 이력서를 대표 이력서로 설정 + AI 요약 요청
     * - 프론트에서 "대표 이력서 설정"을 별도 API로 호출하는 구조면 이 메소드가 필요
     */
    @Transactional
    public void setPrimaryResume(Long resumeId, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);

        if (resume.isPrimary()) return;

        resumeRepository.findByUser_IdAndPrimaryTrue(userId)
                .ifPresent(old -> old.setPrimary(false));

        resume.setPrimary(true);
        resumeRepository.save(resume);

        resumeSummaryService.requestSummary(resume.getId(), userId);
    }

    // --- Helper Methods ---

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private Resume getResumeEntity(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));
    }

    private void validateOwner(Resume resume, Long userId) {
        if (!resume.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to resume");
        }
    }

    private void addChildrenToResume(Resume resume, ResumeRequest request) {
        if (request.getCareers() != null)
            request.getCareers().forEach(d -> resume.getCareers().add(d.toEntity(resume)));
        if (request.getProjects() != null)
            request.getProjects().forEach(d -> resume.getProjects().add(d.toEntity(resume)));
        if (request.getCertificates() != null)
            request.getCertificates().forEach(d -> resume.getCertificates().add(d.toEntity(resume)));
        if (request.getLinks() != null)
            request.getLinks().forEach(d -> resume.getLinks().add(d.toEntity(resume)));
        if (request.getAttachments() != null)
            request.getAttachments().forEach(d -> resume.getAttachments().add(d.toEntity(resume)));
    }

    /**
     * 마이페이지용 프로필 요약 조회
     * 사용자 기본 정보 + 대표 이력서 정보 반환
     */
    public Map<String, Object> getProfileSummary(Long userId) {
        UserEntity user = getUser(userId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("id", user.getId());
        summary.put("display_name", user.getUsername());
        summary.put("email", user.getEmail());
        summary.put("phone", user.getPhone() != null ? user.getPhone() : "");
        summary.put("gender", user.getGender() != null ? user.getGender() : "");
        summary.put("birthday", user.getBirthday() != null ? user.getBirthday() : "");
        summary.put("status", user.getStatus() != null ? user.getStatus() : "");
        summary.put("role", user.getRoleType() != null ? user.getRoleType().name() : "");
        summary.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");

        Optional<Resume> primaryResume = resumeRepository.findByUser_IdAndPrimaryTrue(userId);
        if (primaryResume.isPresent()) {
            Resume resume = primaryResume.get();
            Map<String, Object> resumeSummary = new HashMap<>();
            resumeSummary.put("id", resume.getId());
            resumeSummary.put("title", resume.getTitle());
            resumeSummary.put("field", resume.getField());
            resumeSummary.put("tagline", resume.getTagline());
            resumeSummary.put("careerYears", resume.getCareerYears());
            resumeSummary.put("skills", resume.getReStack() != null ? resume.getReStack() : List.of());
            resumeSummary.put("preferenceLocation", resume.getPreferenceLocation());
            resumeSummary.put("preferenceSalary", resume.getPreferenceSalary());

            if (resume.getProfile() != null) {
                resumeSummary.put("photoUrl", resume.getProfile().getPhotoUrl());
                summary.put("avatar_url", resume.getProfile().getPhotoUrl());
            }

            summary.put("primaryResume", resumeSummary);
            summary.put("job_title", resume.getField());
        } else {
            summary.put("primaryResume", null);
            summary.put("avatar_url", "");
            summary.put("job_title", "");
        }

        // 이력서 개수
        List<Resume> allResumes = resumeRepository.findAllByUser_Id(userId);
        summary.put("resumeCount", allResumes.size());

        // ✅ 내 코드: 스크랩(관심공고) 개수
        long savedJobCount = jobScrapService.getScrapCount(userId);
        summary.put("savedJobCount", savedJobCount);

        return summary;
    }
}
