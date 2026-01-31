package com.example.pproject.application.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.application.dto.JobApplicationRequest;
import com.example.pproject.application.dto.JobApplicationResponse;
import com.example.pproject.application.service.JobApplicationService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Job Application", description = "입사 지원 및 관리 API")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final UserRepository userRepository;

    /** JWT principal에서 회원 PK(member_id) 조회. id 클레임이 있으면 사용, 없으면 userid로 DB 조회 */
    private Long resolveMemberId(JwtUserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (principal.getId() != null) {
            return principal.getId();
        }
        return userRepository.findByUserid(principal.getUserid())
                .map(UserEntity::getId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    @Operation(summary = "입사 지원", description = "공고 ID와 이력서 ID로 입사 지원을 합니다.")
    @PostMapping
    public ResponseEntity<Long> apply(@RequestBody @Valid JobApplicationRequest request,
                                      @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        Long applicationId = jobApplicationService.apply(request, memberId);
        return ResponseEntity.ok(applicationId);
    }

    @Operation(summary = "지원 취소", description = "제출된 지원을 취소합니다. (열람/진행 중일 경우 불가)")
    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> cancelApplication(@PathVariable Long applicationId,
                                                  @AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        jobApplicationService.cancel(applicationId, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 지원 현황 조회", description = "나의 지원 내역과 전형 상태를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<List<JobApplicationResponse>> getMyApplications(@AuthenticationPrincipal JwtUserPrincipal user) {
        Long memberId = resolveMemberId(user);
        return ResponseEntity.ok(jobApplicationService.getMyApplications(memberId));
    }
}