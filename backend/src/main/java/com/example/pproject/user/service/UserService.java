// src/main/java/com/example/pproject/user/service/UserService.java
package com.example.pproject.user.service;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.SocialType;
import com.example.pproject.user.dto.UserRequestDTO;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    // ✅ 서버 기본 notice_id (컨트롤러에서 안 세팅되더라도 최후 방어)
    @Value("${app.notice.terms-id:0}")
    private Long defaultTermsNoticeId;

    @Value("${app.notice.privacy-id:0}")
    private Long defaultPrivacyNoticeId;

    @Value("${app.notice.policy-id:0}")
    private Long defaultPolicyNoticeId;

    @Override
    public UserDetails loadUserByUsername(String userid) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("아이디가 존재하지 않습니다."));

        log.info("{} 사용자 로그인 시도", userEntity);

        return User.builder()
                .username(userEntity.getUserid())
                .password(userEntity.getPassword())
                .roles(userEntity.getRoleType().name())
                .build();
    }

    public void register(UserRequestDTO userDTO) {
        if (userDTO == null) {
            throw new IllegalStateException("요청 데이터가 비어있습니다.");
        }

        // 이메일 형식 검증
        String email = userDTO.getEmail();
        Pattern emailPattern = Pattern.compile(
                "^[A-Za-z0-9]+([._+-][A-Za-z0-9]+)*@[A-Za-z0-9-]+(\\.[A-Za-z]{2,})+$"
        );
        if (email == null || !emailPattern.matcher(email).matches()) {
            throw new IllegalStateException("이메일 형식이 잘못되었습니다.");
        }

        boolean hasPassword = userDTO.getPassword() != null && !userDTO.getPassword().isBlank();
        boolean isSocialSignup = !hasPassword && userDTO.getSocialType() != null;

        // 일반 회원가입: 아이디/비밀번호 필수
        if (!isSocialSignup) {
            if (userDTO.getUserid() == null || userDTO.getUserid().isBlank()) {
                throw new IllegalStateException("아이디를 입력해주세요.");
            }
            if (!hasPassword) {
                throw new IllegalStateException("비밀번호를 입력해주세요.");
            }
            if (userDTO.getPasswordConfirm() == null || !userDTO.getPassword().equals(userDTO.getPasswordConfirm())) {
                throw new IllegalStateException("비밀번호 재확인이 일치하지 않습니다.");
            }
        }

        // 소셜 회원가입: userid가 없으면 내부 식별자 생성
        if (isSocialSignup) {
            if (userDTO.getUserid() == null || userDTO.getUserid().isBlank()) {
                String uid = "social_" + UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8))
                        .toString().replace("-", "");
                userDTO.setUserid(uid);
            }
        }

        // userid 중복 체크
        if (userDTO.getUserid() != null && !userDTO.getUserid().isBlank()) {
            Optional<UserEntity> existingUser = userRepository.findByUserid(userDTO.getUserid());
            if (existingUser.isPresent()) {
                throw new IllegalStateException("이미 존재하는 회원입니다.");
            }
        }

        // 이메일 중복 체크
        Optional<UserEntity> existingEmail = userRepository.findByEmail(userDTO.getEmail());
        if (existingEmail.isPresent()) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }

        // 휴대폰 인증 필수
        String normalizedPhone = userDTO.getPhone() == null ? "" : userDTO.getPhone().replaceAll("[^0-9]", "");
        if (normalizedPhone.isBlank()) {
            throw new IllegalStateException("휴대폰 번호를 입력해주세요.");
        }
        if (userDTO.getPhoneVerifiedAt() == null) {
            throw new IllegalStateException("휴대폰 인증을 완료해주세요.");
        }
        Optional<UserEntity> phoneOwner = userRepository.findFirstByPhoneAndPhoneVerifiedAtIsNotNullAndDeletedAtIsNull(normalizedPhone);
        if (phoneOwner.isPresent()) {
            throw new IllegalStateException("이미 다른 계정에서 인증된 휴대폰 번호입니다.");
        }

        // ✅ 필수 약관 동의(agreed_at 필요)
        if (userDTO.getTermsAgreedAt() == null || userDTO.getPrivacyAgreedAt() == null) {
            throw new IllegalStateException("필수 약관에 동의해야 가입이 가능합니다.");
        }

        // ✅ DB 제약(ck_member_required_consents) 통과를 위한 notice_id 보장
        Long tId = (userDTO.getTermsNoticeId() != null) ? userDTO.getTermsNoticeId() : defaultTermsNoticeId;
        Long pId = (userDTO.getPrivacyNoticeId() != null) ? userDTO.getPrivacyNoticeId() : defaultPrivacyNoticeId;
        Long oId = (userDTO.getPolicyNoticeId() != null) ? userDTO.getPolicyNoticeId() : defaultPolicyNoticeId;

        if (tId == null || tId <= 0 || pId == null || pId <= 0) {
            throw new IllegalStateException("약관 문서(TERMS/PRIVACY)가 준비되지 않았습니다. (notice_id 설정 필요)");
        }

        UserEntity userEntity = modelMapper.map(userDTO, UserEntity.class);

        // normalize phone
        userEntity.setPhone(normalizedPhone);

        // 비밀번호 암호화
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            userEntity.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        } else {
            userEntity.setPassword(null);
        }

        // 기본 권한
        if (userEntity.getRoleType() == null) {
            userEntity.setRoleType(RoleType.CANDIDATE);
        }

        // 소셜 타입 기본
        userEntity.setSocialType(userDTO.getSocialType() != null ? userDTO.getSocialType() : SocialType.OTHER);

        // 마케팅 동의 null 방지
        if (userEntity.getMarketingOptIn() == null) {
            userEntity.setMarketingOptIn(false);
        }

        // ✅ notice_id 강제 세팅 (ModelMapper가 누락해도 최종 보장)
        userEntity.setTermsNoticeId(tId);
        userEntity.setPrivacyNoticeId(pId);
        if (oId != null && oId > 0) {
            userEntity.setPolicyNoticeId(oId);
        }

        userRepository.save(userEntity);
    }

    public UserEntity findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일로 가입된 계정이 없습니다."));
    }

    public void assertEmailExists(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("이메일을 입력해주세요.");
        }
        Optional<UserEntity> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new IllegalStateException("해당 이메일로 가입된 계정이 없습니다.");
        }
        if (user.get().getPassword() == null || user.get().getPassword().isBlank()) {
            throw new IllegalStateException("소셜 회원은 아이디 찾기를 이용할 수 없습니다.");
        }
    }

    public String findUseridByEmail(String email) {
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일로 가입된 계정이 없습니다."));

        if (userEntity.getPassword() == null || userEntity.getPassword().isBlank()) {
            throw new IllegalStateException("소셜 회원은 아이디 찾기를 이용할 수 없습니다.");
        }
        return userEntity.getUserid();
    }

    public boolean verifyPassword(String userid, String password) {
        UserEntity userEntity = userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userid));

        return passwordEncoder.matches(password, userEntity.getPassword());
    }

    public void updatePassword(String userid, String newPassword) {
        UserEntity userEntity = userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userid));

        userEntity.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userEntity);
    }

    public String findEmailByUseridAndUsernameAndBirthday(String userid, String username, String birthday) {
        UserEntity userEntity = userRepository.findByUseridAndUsernameAndBirthday(userid, username, birthday)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (userEntity.getPassword() == null || userEntity.getPassword().isBlank()) {
            throw new RuntimeException("소셜 회원은 비밀번호 찾기를 이용할 수 없습니다.");
        }
        return userEntity.getEmail();
    }

    public String findEmailByUseridAndUsernameAndEmailForPasswordLink(String userid, String username, String email) {
        UserEntity userEntity = userRepository.findByUseridAndUsernameAndEmail(userid, username, email)
                .orElseThrow(() -> new RuntimeException("정보가 일치하지 않습니다."));

        if (userEntity.getPassword() == null || userEntity.getPassword().isBlank()) {
            throw new RuntimeException("소셜 회원은 비밀번호 찾기를 이용할 수 없습니다.");
        }

        return userEntity.getEmail();
    }
}