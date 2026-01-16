package com.example.pproject.resume.service;

import com.example.pproject.resume.dto.ResumeRequest;
import com.example.pproject.resume.dto.ResumeResponse;
import com.example.pproject.resume.entity.*;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    // 이력서 목록 조회
    public List<ResumeResponse> getResumes(Integer userId) {
        return resumeRepository.findAllByUserId(userId).stream()
                .map(ResumeResponse::from)
                .collect(Collectors.toList());
    }

    // 대표 이력서 조회
    public ResumeResponse getPrimaryResume(Integer userId) {
        Resume resume = resumeRepository.findByUserIdAndPrimaryTrue(userId)
                .orElseThrow(() -> new IllegalArgumentException("대표 이력서가 설정되지 않았습니다."));
        return ResumeResponse.from(resume);
    }

    // 이력서 작성
    @Transactional
    public Long createResume(ResumeRequest request, Integer userId) {
        UserEntity user = getUser(userId);
        Resume resume = request.toEntity(user);

        // 첫 이력서라면 자동으로 대표 이력서로 설정 (옵션)
        boolean isFirst = resumeRepository.findAllByUserId(userId).isEmpty();
        if (isFirst) {
            resume.setPrimary(true);
        }

        if (request.getProfile() != null) {
            resume.updateProfile(request.getProfile().toEntity(resume));
        }
        addChildrenToResume(resume, request);

        return resumeRepository.save(resume).getId();
    }

    // 이력서 상세 조회
    public ResumeResponse getResume(Long resumeId) {
        Resume resume = getResumeEntity(resumeId);
        return ResumeResponse.from(resume);
    }

    // 이력서 수정 (대표 설정 및 기본 정보)
    @Transactional
    public Long updateResume(Long resumeId, ResumeRequest request, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);

        // 1. 기본 정보 수정
        resume.updateInfo(
                request.getTitle(),
                request.getTagline(),
                request.getContent(),
                request.getPublicOption(),
                request.getField(),
                request.getPreferenceLocation(),
                request.getPreferenceSalary(),
                request.getEmploymentType(),
                request.getReStack()
        );

        // 2. 대표 이력서 설정 로직
        if (Boolean.TRUE.equals(request.getPrimary())) {
            // 기존 대표 이력서가 있다면 해제
            resumeRepository.findByUserIdAndPrimaryTrue(userId)
                    .ifPresent(oldPrimary -> oldPrimary.setPrimary(false));

            // 현재 이력서를 대표로 설정
            resume.setPrimary(true);
        }

        return resume.getId();
    }

    // 이력서 삭제
    @Transactional
    public void deleteResume(Long resumeId, Integer userId) {
        Resume resume = getResumeEntity(resumeId);
        validateOwner(resume, userId);
        resumeRepository.delete(resume);
    }

    // 이력서 복사
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
                .reStack(original.getReStack())
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

    // 하위 항목 추가
    @Transactional public void addCareer(Long id, ResumeRequest.CareerDto dto, Integer uid) {
        Resume r = getResumeEntity(id); validateOwner(r, uid); r.getCareers().add(dto.toEntity(r));
    }
    @Transactional public void addProject(Long id, ResumeRequest.ProjectDto dto, Integer uid) {
        Resume r = getResumeEntity(id); validateOwner(r, uid); r.getProjects().add(dto.toEntity(r));
    }
    @Transactional public void addLink(Long id, ResumeRequest.LinkDto dto, Integer uid) {
        Resume r = getResumeEntity(id); validateOwner(r, uid); r.getLinks().add(dto.toEntity(r));
    }
    @Transactional public void addAttachment(Long id, ResumeRequest.AttachmentDto dto, Integer uid) {
        Resume r = getResumeEntity(id); validateOwner(r, uid); r.getAttachments().add(dto.toEntity(r));
    }

    private UserEntity getUser(Integer userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
    private Resume getResumeEntity(Long resumeId) {
        return resumeRepository.findById(resumeId).orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));
    }
    private void validateOwner(Resume resume, Integer userId) {
        if (!resume.getUser().getId().toString().equals(userId.toString())) throw new IllegalArgumentException("Unauthorized");
    }
    private void addChildrenToResume(Resume resume, ResumeRequest request) {
        if(request.getCareers() != null) request.getCareers().forEach(d->resume.getCareers().add(d.toEntity(resume)));
        if(request.getProjects() != null) request.getProjects().forEach(d->resume.getProjects().add(d.toEntity(resume)));
        if(request.getCertificates() != null) request.getCertificates().forEach(d->resume.getCertificates().add(d.toEntity(resume)));
        if(request.getLinks() != null) request.getLinks().forEach(d->resume.getLinks().add(d.toEntity(resume)));
        if(request.getAttachments() != null) request.getAttachments().forEach(d->resume.getAttachments().add(d.toEntity(resume)));
    }
}