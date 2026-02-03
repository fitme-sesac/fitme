package com.example.pproject.resume.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.resume.dto.ResumeRequest;
import com.example.pproject.resume.dto.ResumeResponse;
import com.example.pproject.resume.service.ResumeService;
import com.example.pproject.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Tag(name = "Resume", description = "이력서 및 포트폴리오 관리 API")
public class ResumeController {

    private final ResumeService resumeService;
    private final UserRepository userRepository;

    /** JWT에 id가 없을 때 userid로 DB 조회하여 memberId 반환 (구 토큰 호환) */
    private Long resolveMemberId(JwtUserPrincipal user) {
        if (user == null) return null;
        if (user.getId() != null) return user.getId();
        if (user.getUserid() != null && !user.getUserid().isBlank()) {
            return userRepository.findByUserid(user.getUserid())
                    .map(u -> u.getId())
                    .orElse(null);
        }
        return null;
    }

    /**
     * 나의 이력서 목록 조회
     * 설명: 로그인한 사용자의 모든 이력서 목록을 조회합니다. (대표 이력서 여부 포함)
     */
    @Operation(summary = "내 이력서 목록 조회", description = "사용자의 이력서 목록을 반환합니다. 대표 이력서(is_primary) 여부를 확인할 수 있습니다.")
    @GetMapping
    public ResponseEntity<List<ResumeResponse>> getMyResumes(@AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(resumeService.getResumes(memberId));
    }

    /**
     * 이력서 상세 조회
     * 설명: 경력, 프로젝트, 자격증, 링크 등 모든 하위 정보를 포함하여 반환합니다.
     */
    @Operation(summary = "이력서 상세 조회", description = "특정 이력서의 상세 정보(경력, 프로젝트, 자격증 등 포함)를 조회합니다.")
    @GetMapping("/{resumeId}")
    public ResponseEntity<ResumeResponse> getResume(@PathVariable Long resumeId) {
        return ResponseEntity.ok(resumeService.getResume(resumeId));
    }

    /**
     * 새 이력서 생성
     * 설명: 제목, 직무 분야 등 기본 정보를 받아 새 이력서를 생성합니다.
     */
    @Operation(summary = "새 이력서 생성", description = "새로운 이력서를 작성합니다. 첫 이력서일 경우 자동으로 대표 이력서로 설정됩니다.")
    @PostMapping
    public ResponseEntity<Long> createResume(@RequestBody ResumeRequest request,
                                             @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(resumeService.createResume(request, memberId));
    }

    /**
     * 이력서 수정
     * 설명: 기본 정보 수정 및 대표 이력서(is_primary) 설정을 수행합니다.
     */
    @Operation(summary = "이력서 수정", description = "이력서의 기본 정보(제목, 자기소개 등)를 수정하거나 대표 이력서로 설정합니다.")
    @PatchMapping("/{resumeId}")
    public ResponseEntity<Long> updateResume(@PathVariable Long resumeId,
                                             @RequestBody ResumeRequest request,
                                             @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(resumeService.updateResume(resumeId, request, memberId));
    }

    /**
     * 이력서 삭제
     * 설명: 이력서를 삭제합니다. (지원 내역이 있는 경우 삭제가 제한될 수 있습니다.)
     */
    @Operation(summary = "이력서 삭제", description = "이력서를 삭제합니다. 단, 이미 지원한 내역이 있는 경우 삭제가 불가능할 수 있습니다.")
    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long resumeId,
                                             @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.deleteResume(resumeId, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 이력서 복제
     * 설명: 기존 이력서의 내용을 그대로 복사하여 새로운 이력서를 생성합니다.
     */
    @Operation(summary = "이력서 복제", description = "기존 이력서의 모든 하위 데이터(경력, 프로젝트 등)를 복사하여 새 이력서를 생성합니다.")
    @PostMapping("/{resumeId}/copy")
    public ResponseEntity<Long> copyResume(@PathVariable Long resumeId,
                                           @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(resumeService.copyResume(resumeId, memberId));
    }

    @Operation(summary = "경력 추가", description = "이력서에 경력 사항을 추가합니다.")
    @PostMapping("/{resumeId}/careers")
    public ResponseEntity<Void> addCareer(@PathVariable Long resumeId,
                                          @RequestBody ResumeRequest.CareerDto dto,
                                          @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.addCareer(resumeId, dto, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "경력 삭제", description = "특정 경력 사항을 삭제합니다.")
    @DeleteMapping("/{resumeId}/careers/{careerId}")
    public ResponseEntity<Void> deleteCareer(@PathVariable Long resumeId,
                                             @PathVariable Long careerId,
                                             @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.deleteCareer(resumeId, careerId, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로젝트 경험 추가", description = "이력서에 프로젝트 수행 경험을 추가합니다.")
    @PostMapping("/{resumeId}/projects")
    public ResponseEntity<Void> addProject(@PathVariable Long resumeId,
                                           @RequestBody ResumeRequest.ProjectDto dto,
                                           @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.addProject(resumeId, dto, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로젝트 경험 삭제", description = "특정 프로젝트 경험을 삭제합니다.")
    @DeleteMapping("/{resumeId}/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long resumeId,
                                              @PathVariable Long projectId,
                                              @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.deleteProject(resumeId, projectId, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "자격증 추가", description = "이력서에 자격증 정보를 추가합니다.")
    @PostMapping("/{resumeId}/certificates")
    public ResponseEntity<Void> addCertificate(@PathVariable Long resumeId,
                                               @RequestBody ResumeRequest.CertificateDto dto,
                                               @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.addCertificate(resumeId, dto, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "자격증 삭제", description = "특정 자격증 정보를 삭제합니다.")
    @DeleteMapping("/{resumeId}/certificates/{certId}")
    public ResponseEntity<Void> deleteCertificate(@PathVariable Long resumeId,
                                                  @PathVariable Long certId,
                                                  @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.deleteCertificate(resumeId, certId, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "링크 추가", description = "이력서에 깃허브, 블로그 등의 링크를 추가합니다.")
    @PostMapping("/{resumeId}/links")
    public ResponseEntity<Void> addLink(@PathVariable Long resumeId,
                                        @RequestBody ResumeRequest.LinkDto dto,
                                        @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.addLink(resumeId, dto, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "링크 삭제", description = "특정 링크를 삭제합니다.")
    @DeleteMapping("/{resumeId}/links/{linkId}")
    public ResponseEntity<Void> deleteLink(@PathVariable Long resumeId,
                                           @PathVariable Long linkId,
                                           @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.deleteLink(resumeId, linkId, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이력서 프로필 사진 등록", description = "이력서 상단에 표시될 프로필 사진 URL을 등록합니다.")
    @PostMapping("/{resumeId}/image")
    public ResponseEntity<Void> updateProfileImage(@PathVariable Long resumeId,
                                                   @RequestBody Map<String, String> body, // {"photoUrl": "..."}
                                                   @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String photoUrl = body.get("photoUrl");
        resumeService.updateProfileImage(resumeId, photoUrl, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 포트폴리오 파일 업로드 정보 저장
     * 설명: 파일 서버에 저장된 포트폴리오 파일의 URL과 메타데이터를 DB에 저장합니다.
     */
    @Operation(summary = "포트폴리오 파일 등록", description = "업로드된 포트폴리오 파일의 URL 및 메타데이터를 저장합니다. (실제 파일 업로드는 별도 수행)")
    @PostMapping("/{resumeId}/attachments")
    public ResponseEntity<Void> addAttachment(@PathVariable Long resumeId,
                                              @RequestBody ResumeRequest.AttachmentDto dto,
                                              @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.addAttachment(resumeId, dto, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 포트폴리오 파일 삭제
     * 설명: 업로드된 포트폴리오 파일 정보를 삭제합니다.
     */
    @Operation(summary = "포트폴리오 파일 삭제", description = "등록된 포트폴리오 파일을 삭제합니다.")
    @DeleteMapping("/{resumeId}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long resumeId,
                                                 @PathVariable Long attachmentId,
                                                 @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        resumeService.deleteAttachment(resumeId, attachmentId, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 내 프로필 요약 조회 (마이페이지용)
     * 설명: 사용자 기본 정보와 대표 이력서 요약 정보를 반환합니다.
     */
    @Operation(summary = "내 프로필 요약 조회", description = "마이페이지에 표시할 사용자 기본 정보와 대표 이력서 요약을 반환합니다.")
    @GetMapping("/my-profile-summary")
    public ResponseEntity<Map<String, Object>> getMyProfileSummary(@AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Map<String, Object> summary = resumeService.getProfileSummary(memberId);
        return ResponseEntity.ok(summary);
    }
}
