package com.example.pproject.auth.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class AuthStatusController {

    private final UserRepository userRepository;

    @GetMapping("/api/auth/status")
    public Map<String, Object> status(Authentication auth) {

        boolean jwtAuthenticated = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && (auth.getPrincipal() instanceof JwtUserPrincipal);

        if (!jwtAuthenticated) {
            Map<String, Object> result = new HashMap<>();
            result.put("authenticated", false);
            result.put("name", "");
            result.put("role", "");
            return result;
        }

        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();

        // role 추출 (ROLE_ 접두사 제거)
        String role = p.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("");

        // DB에서 추가 사용자 정보 조회
        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", true);
        result.put("role", role);

        // 기본값 설정
        String displayName = (p.getDisplayName() == null || p.getDisplayName().isBlank())
                ? p.getUserid()
                : p.getDisplayName();
        result.put("name", displayName);
        result.put("userid", p.getUserid());
        result.put("email", p.getEmail() != null ? p.getEmail() : "");

        // DB에서 상세 정보 조회 (id가 있으면 id로, 없으면 userid로)
        Optional<UserEntity> userOpt = Optional.empty();
        if (p.getId() != null) {
            userOpt = userRepository.findById(p.getId());
        }
        if (userOpt.isEmpty() && p.getUserid() != null) {
            userOpt = userRepository.findByUserid(p.getUserid());
        }
        if (userOpt.isEmpty() && p.getEmail() != null) {
            userOpt = userRepository.findByEmail(p.getEmail());
        }

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            result.put("id", String.valueOf(user.getId()));
            result.put("name", user.getUsername()); // DB의 실제 이름
            // 이메일: DB 우선, 없으면 JWT 값
            String email = (user.getEmail() != null && !user.getEmail().isBlank())
                    ? user.getEmail()
                    : (p.getEmail() != null && !p.getEmail().isBlank() ? p.getEmail() : "");
            result.put("email", email);
            result.put("phone", user.getPhone() != null ? user.getPhone() : "");
            result.put("gender", user.getGender() != null ? user.getGender() : "");
            result.put("birthday", user.getBirthday() != null ? user.getBirthday() : "");
            result.put("status", user.getStatus() != null ? user.getStatus() : "");
            result.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");

            // user_metadata 형태로도 제공 (프론트엔드 호환)
            Map<String, Object> userMetadata = new HashMap<>();
            userMetadata.put("name", user.getUsername());
            userMetadata.put("display_name", user.getUsername());
            userMetadata.put("avatar_url", ""); // 프로필 이미지 URL이 있다면 여기에 추가
            userMetadata.put("job_title", ""); // 직함이 있다면 여기에 추가
            result.put("user_metadata", userMetadata);
        } else {
            // DB에서 찾지 못한 경우 JWT의 정보 사용
            Map<String, Object> userMetadata = new HashMap<>();
            userMetadata.put("name", displayName);
            userMetadata.put("display_name", displayName);
            userMetadata.put("avatar_url", "");
            userMetadata.put("job_title", "");
            result.put("user_metadata", userMetadata);
        }

        return result;
    }
}
