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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;

    // 1. 내 이력서 목록 조회
    public List<ResumeResponse> getResumes(Integer userId) {
        return resumeRepository.findAllByUserId(userId).stream()
                .map(ResumeResponse::from)
                .collect(Collectors.toList());
    }

    // 2. 이력서 상세 조회
    public ResumeResponse getResume(Long resumeId) {
        Resume resume = getResumeEntity(resumeId);
        return ResumeResponse.from(resume);
    }

    // 3. 이력서 생성
    @Transactional
    public Long createResume(ResumeRequest request, Integer userId) {
        UserEntity user = getUser(userId);
        Resume resume = request.toEntity(user);

        if (resumeRepository.findAllByUserId(userId).isEmpty()) {
            resume.setPrimary(true);
        }

        if (request.getProfile() != null) {
            resume.updateProfile(request.getProfile().toEntity(resume));
        }

        addChildrenToResume(resume, request);

        return resumeRepository.save(resume).getId();
    }

    // 4. 이력서 수정
    @Transactional
    public Long updateResume(Long resumeId, ResumeRequest request, Integer userId) {
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

        if (Boolean.TRUE.equals(request.getPrimary())) {
            resumeRepository.findByUserIdAndPrimaryTrue(userId)
                    .ifPresent(old -> old.setPrimary(false));
            resume.setPrimary(true);
        }

        return resume.getId();
    }

    // 5. 이력서 삭제
    @Transactional
    public void deleteResume(Long resumeId, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);

        boolean hasApplication = jobApplicationRepository.existsByResumeId(resumeId);
        if (hasApplication) {
            throw new IllegalStateException("이미 입사 지원에 사용된 이력서는 삭제할 수 없습니다.");
        }

        resumeRepository.delete(resume);
    }

    // 6. 이력서 복제
    @Transactional
    public Long copyResume(Long resumeId, Integer userId) {
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
                .reStack(original.getReStack() != null ? new ArrayList<>(original.getReStack()) : new ArrayList<>()) // Deep Copy
                .careerYears(original.getCareerYears()) // 연차 복사
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

        // 하위 항목 복사
        if (original.getCareers() != null) {
            original.getCareers().forEach(c -> copy.getCareers().add(ResumeCareer.builder()
                    .resume(copy)
                    .companyName(c.getCompanyName())
                    .department(c.getDepartment())
                    .role(c.getRole())
                    .startDate(c.getStartDate())
                    .endDate(c.getEndDate())
                    .current(c.getCurrent())
                    .verified(c.getVerified())
                    .build()));
        }
        if (original.getProjects() != null) {
            original.getProjects().forEach(p -> copy.getProjects().add(ResumeProject.builder()
                    .resume(copy)
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .techStack(p.getTechStack())
                    .startDate(p.getStartDate())
                    .endDate(p.getEndDate())
                    .contributionPct(p.getContributionPct())
                    .sortOrder(p.getSortOrder())
                    .build()));
        }
        if (original.getCertificates() != null) {
            original.getCertificates().forEach(c -> copy.getCertificates().add(ResumeCertificate.builder()
                    .resume(copy)
                    .name(c.getName())
                    .issuer(c.getIssuer())
                    .acquisitionDate(c.getAcquisitionDate())
                    .verified(c.getVerified())
                    .build()));
        }
        if (original.getLinks() != null) {
            original.getLinks().forEach(l -> copy.getLinks().add(ResumeLink.builder()
                    .resume(copy)
                    .linkType(l.getLinkType())
                    .url(l.getUrl())
                    .build()));
        }
        // 첨부파일은 물리 파일 복사가 필요할 수 있으나, 여기서는 DB 메타데이터만 복사하거나 정책에 따라 제외
        // (필요 시 Attachment 복사 로직 추가)

        return resumeRepository.save(copy).getId();
    }

    // 하위 항목 관리
    @Transactional
    public void addCareer(Long resumeId, ResumeRequest.CareerDto dto, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getCareers().add(dto.toEntity(resume));
    }
    @Transactional
    public void deleteCareer(Long resumeId, Long careerId, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getCareers().removeIf(c -> c.getId().equals(careerId));
    }
    @Transactional
    public void addProject(Long resumeId, ResumeRequest.ProjectDto dto, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getProjects().add(dto.toEntity(resume));
    }
    @Transactional
    public void deleteProject(Long resumeId, Long projectId, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getProjects().removeIf(p -> p.getId().equals(projectId));
    }
    @Transactional
    public void addCertificate(Long resumeId, ResumeRequest.CertificateDto dto, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getCertificates().add(dto.toEntity(resume));
    }
    @Transactional
    public void deleteCertificate(Long resumeId, Long certId, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getCertificates().removeIf(c -> c.getId().equals(certId));
    }
    @Transactional
    public void addLink(Long resumeId, ResumeRequest.LinkDto dto, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getLinks().add(dto.toEntity(resume));
    }
    @Transactional
    public void deleteLink(Long resumeId, Long linkId, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getLinks().removeIf(l -> l.getId().equals(linkId));
    }
    @Transactional
    public void addAttachment(Long resumeId, ResumeRequest.AttachmentDto dto, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getAttachments().add(dto.toEntity(resume));
    }
    @Transactional
    public void deleteAttachment(Long resumeId, Long attachmentId, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resume.getAttachments().removeIf(a -> a.getId().equals(attachmentId));
    }

    // 프로필 사진 업데이트
    @Transactional
    public void updateProfileImage(Long resumeId, String photoUrl, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);

        ResumeProfile profile = resume.getProfile();
        if (profile == null) {
            // 프로필이 없으면 새로 생성
            profile = ResumeProfile.builder()
                    .resume(resume)
                    .photoUrl(photoUrl)
                    .build();
            resume.updateProfile(profile);
        } else {
            // 프로필이 있으면 photoUrl만 업데이트
            profile.updatePhotoUrl(photoUrl);
        }
    }

    private UserEntity getUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
    private Resume getResumeEntity(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));
    }
    private void validateOwner(Resume resume, Integer userId) {
        if (!resume.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to resume");
        }
    }
    private void addChildrenToResume(Resume resume, ResumeRequest request) {
        if(request.getCareers() != null) request.getCareers().forEach(d -> resume.getCareers().add(d.toEntity(resume)));
        if(request.getProjects() != null) request.getProjects().forEach(d -> resume.getProjects().add(d.toEntity(resume)));
        if(request.getCertificates() != null) request.getCertificates().forEach(d -> resume.getCertificates().add(d.toEntity(resume)));
        if(request.getLinks() != null) request.getLinks().forEach(d -> resume.getLinks().add(d.toEntity(resume)));
        if(request.getAttachments() != null) request.getAttachments().forEach(d -> resume.getAttachments().add(d.toEntity(resume)));
    }
}