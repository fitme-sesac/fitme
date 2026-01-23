package com.example.pproject.user.service;

import com.example.pproject.application.repository.JobApplicationRepository;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.entity.ResumeProfile;
import com.example.pproject.resume.repository.ResumeRepository;
import com.example.pproject.user.dto.UserRequestDTO;
import com.example.pproject.user.dto.UserResponseDTO;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import com.example.pproject.wallet.entity.Wallet;
import com.example.pproject.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class MyPageService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final JobApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;

    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 1. 마이페이지 대시보드 조회
     * - 회원 기본 정보 + 지갑 잔액 + (대표 이력서의 주소 정보) 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMyDashboard(Long userId) {
        // 1) 사용자 조회 (Login ID로 조회)
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 2) 기본 정보 매핑
        UserResponseDTO userInfo = modelMapper.map(user, UserResponseDTO.class);

        // 3) [주소 연동] 대표 이력서에서 주소 정보 가져와서 DTO에 채우기 (Long ID 사용)
        // ResumeRepository에 정의된 표준 메소드(findByUser_IdAndPrimaryTrue) 호출
        Optional<Resume> primaryResume = resumeRepository.findByUser_IdAndPrimaryTrue(user.getId());

        if (primaryResume.isPresent() && primaryResume.get().getProfile() != null) {
            ResumeProfile profile = primaryResume.get().getProfile();
            userInfo.setAddress(profile.getAddress());
        }

        // 4) 지갑 잔액 조회 (WalletRepository 구현에 따라 findByMember 혹은 findByMemberId 사용)
        // 여기서는 UserEntity의 ID(Long)를 사용하여 조회
        long creditBalance = walletRepository.findByMember(user)
                .map(Wallet::getBalance).orElse(0L);

        // 5) 통계 데이터 구성
        long applicationCount = applicationRepository.countByMemberId(user.getId());

        // ResumeRepository에 countByUser_Id가 있다면 사용, 없다면 findAllByUser_Id(user.getId()).size() 사용
        // 앞서 ResumeRepository에 countByUser_Id를 추가했다고 가정
        long resumeCount = resumeRepository.countByUser_Id(user.getId());

        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("profile", userInfo);
        dashboardData.put("creditBalance", creditBalance);
        dashboardData.put("stats", Map.of(
                "applicationCount", applicationCount,
                "resumeCount", resumeCount
        ));

        return dashboardData;
    }

    /**
     * 2. 내 정보 수정
     * - 회원 테이블 정보 수정 + (대표 이력서의 주소 정보 수정)
     */
    public void updateMyInfo(Long userId, UserRequestDTO requestDTO) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 1) Member 테이블 필드 수정
        if (requestDTO.getUsername() != null && !requestDTO.getUsername().isBlank()) {
            user.setUsername(requestDTO.getUsername());
        }
        if (requestDTO.getPhone() != null && !requestDTO.getPhone().isBlank()) {
            user.setPhone(requestDTO.getPhone().replaceAll("[^0-9]", ""));
        }
        if (requestDTO.getPassword() != null && !requestDTO.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        }
        if (requestDTO.getMarketingOptIn() != null && !requestDTO.getMarketingOptIn().equals(user.getMarketingOptIn())) {
            user.setMarketingOptIn(requestDTO.getMarketingOptIn());
            user.setMarketingAgreedAt(Boolean.TRUE.equals(requestDTO.getMarketingOptIn()) ? Instant.now() : null);
        }

        // 2) [주소 정보 업데이트] 대표 이력서 찾아서 업데이트 (Long ID 사용)
        if (requestDTO.getAddress() != null) {
            resumeRepository.findByUser_IdAndPrimaryTrue(user.getId())
                    .ifPresent(resume -> {
                        // 이력서에 프로필이 없으면 생성
                        if (resume.getProfile() == null) {
                            ResumeProfile newProfile = ResumeProfile.builder()
                                    .resume(resume)
                                    .build();
                            resume.updateProfile(newProfile);
                        }

                        // 주소 문자열 조합
                        String fullAddress = requestDTO.getAddress();
                        if (requestDTO.getDetailAddress() != null && !requestDTO.getDetailAddress().isBlank()) {
                            fullAddress += " " + requestDTO.getDetailAddress();
                        }

                        // Profile 엔티티 업데이트
                        resume.getProfile().updateAddress(fullAddress);
                    });
        }
    }

    /**
     * 3. 회원 탈퇴
     */
    public void withdrawUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getDeletedAt() != null) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        user.setDeletedAt(Instant.now());
        // 상태값 변경 로직 (Enum 처리 필요 시 수정)
        user.setStatus("WITHDRAWN");

        log.info("회원 탈퇴 처리 완료: userId={}", userId);
    }
}
