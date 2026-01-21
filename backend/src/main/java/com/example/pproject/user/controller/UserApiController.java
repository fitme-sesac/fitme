package com.example.pproject.user.controller;

import com.example.pproject.Config.JwtTokenProvider;
import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.user.dto.UserRequestDTO;
import com.example.pproject.user.dto.UserResponseDTO;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 회원 관련 REST API 컨트롤러
 * - 회원 정보 조회/수정 기능 제공
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Log4j2
public class UserApiController {

    private final UserService userService;
    private final ModelMapper modelMapper;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 현재 로그인한 사용자의 회원 정보 조회
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal JwtUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            UserEntity user = userService.findByUseridOrThrow(principal.getUserid());
            UserResponseDTO response = modelMapper.map(user, UserResponseDTO.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("회원 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "회원 정보 조회에 실패했습니다."));
        }
    }

    /**
     * 회원 정보 수정
     * PUT /api/user/profile
     * 
     * - ID, 이름, 성별, 생일은 변경 불가 (프론트에서 disabled 처리)
     * - 비밀번호는 필수 입력 (본인 확인용)
     * - 휴대폰 번호 변경 시 재인증 필요
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody UserRequestDTO dto,
            @CookieValue(value = "PHONE_VERIFIED_TMP", required = false) String phoneVerifiedToken
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            // 휴대폰 인증 토큰에서 인증된 번호 추출 (변경 시에만 필요)
            String verifiedPhone = PhoneOtpController.readVerifiedPhone(jwtTokenProvider, phoneVerifiedToken, "PROFILE_UPDATE");
            
            userService.updateUserInfo(principal.getUserid(), dto, verifiedPhone);
            
            Map<String, Object> result = new HashMap<>();
            result.put("ok", true);
            result.put("message", "회원 정보가 수정되었습니다.");
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("회원 정보 수정 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "회원 정보 수정에 실패했습니다."));
        }
    }

    /**
     * 비밀번호 변경 (회원정보 수정 페이지에서)
     * PUT /api/user/password
     */
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody Map<String, String> body
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        if (currentPassword == null || currentPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "현재 비밀번호를 입력해주세요."));
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "새 비밀번호를 입력해주세요."));
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "새 비밀번호와 확인이 일치하지 않습니다."));
        }

        try {
            userService.updatePasswordWithVerification(principal.getUserid(), currentPassword, newPassword);
            return ResponseEntity.ok(Map.of("ok", true, "message", "비밀번호가 변경되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "비밀번호 변경에 실패했습니다."));
        }
    }
}
