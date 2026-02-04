package com.example.pproject.resume.service;

import com.example.pproject.application.repository.JobApplicationRepository;
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
    private final ResumeSummaryService resumeSummaryService; // AI 요약 서비스 추가

    // 1. 내 이력서 목록 조회 (Long userId)
    public List<ResumeResponse> getResumes(Long userId) {
        // findAllByUser_Id : Long 타입 ID를 지원하는 표준 메소드 사용
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

        // 첫 이력서라면 대표 이력서로 설정 (findAllByUser_Id 사용)
        if (resumeRepository.findAllByUser_Id(userId).isEmpty()) {
            resume.setPrimary(true);
            // AI 요약/임베딩 요청 (비동기)
            resumeSummaryService.requestSummary(resume.getId(), userId);
        }

        // 프로필(사진, 주소) 저장
        if (request.getProfile() != null) {
            resume.updateProfile(request.getProfile().toEntity(resume));
        }

        // 자식 엔티티 추가 (경력, 프로젝트 등)
        addChildrenToResume(resume, request);

        return resumeRepository.save(resume).getId();
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
                request.getReStack(), // List<String>
                request.getCareerYears(), // Integer
                request.getSchool(), request.getSchoolState(), request.getSchoolClass());

        // 대표 이력서 변경 로직 (findByUser_IdAndPrimaryTrue 사용)
        // 대표 이력서 변경 로직 (findByUser_IdAndPrimaryTrue 사용)
        if (Boolean.TRUE.equals(request.getPrimary())) {
            resumeRepository.findByUser_IdAndPrimaryTrue(userId)
                    .ifPresent(old -> old.setPrimary(false));
            resume.setPrimary(true);

            // AI 요약/임베딩 요청 (비동기)
            // 기존 이력서가 변경되었으므로 내용을 다시 분석해야 함
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

        // (선택사항) 자식 데이터(경력, 프로젝트 등) 복제 로직 필요 시 여기에 추가
        // copyChildren(original, copy);

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

    // --- Helper Methods ---

    /**
     * [NEW] 특정 이력서를 대표 이력서로 설정 + AI 요약 요청
     */
    @Transactional
    public void setPrimaryResume(Long resumeId, Long userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);

        if (resume.isPrimary()) {
            return; // 이미 대표 이력서임
        }

        // 1. 기존 대표 해제
        resumeRepository.findByUser_IdAndPrimaryTrue(userId)
                .ifPresent(old -> old.setPrimary(false));

        // 2. 새 대표 설정
        resume.setPrimary(true);
        // 저장은 Transaction Commit 시 dirty checking으로 발생하지만, 명시적 save도 무방
        resumeRepository.save(resume);

        // 3. AI 요약/임베딩 요청 (비동기)
        resumeSummaryService.requestSummary(resume.getId(), userId);
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private Resume getResumeEntity(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));
    }

    private void validateOwner(Resume resume, Long userId) {
        // ID 비교는 equals로 안전하게 처리 (Long 객체끼리 비교)
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

        // 대표 이력서 정보
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
            summary.put("job_title", resume.getField()); // 직무 분야를 job_title로 표시
        } else {
            summary.put("primaryResume", null);
            summary.put("avatar_url", "");
            summary.put("job_title", "");
        }

        // 이력서 개수
        List<Resume> allResumes = resumeRepository.findAllByUser_Id(userId);
        summary.put("resumeCount", allResumes.size());

        return summary;
    }
}
