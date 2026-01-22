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

import java.time.LocalDateTime;
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
    public Map<String, Object> getMyDashboard(String userid) {
        // 1) 사용자 조회
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 2) 기본 정보 매핑
        UserResponseDTO userInfo = modelMapper.map(user, UserResponseDTO.class);

        // 3) [주소 연동] 대표 이력서에서 주소 정보 가져와서 DTO에 채우기
        Optional<Resume> primaryResume = resumeRepository.findByUserIdAndPrimaryTrue(user.getId());

        if (primaryResume.isPresent() && primaryResume.get().getProfile() != null) {
            ResumeProfile profile = primaryResume.get().getProfile();
            userInfo.setAddress(profile.getAddress());
            // 필요한 경우 상세 주소나 우편번호 로직도 여기에 추가 가능
        }

        // 4) 지갑 잔액 조회
        Long userDbId = Long.valueOf(user.getId());
        long creditBalance = walletRepository.findByMember(userDbId)
                .map(Wallet::getBalance).orElse(0L);

        // 5) 통계 데이터 구성
        // (필요 시 Repository에 count 메소드 추가 후 주석 해제하여 사용)
        long applicationCount = 0; // applicationRepository.countByMemberId(userDbId);
        long resumeCount = 0;      // resumeRepository.countByUserId(user.getId());

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
    public void updateMyInfo(String userid, UserRequestDTO requestDTO) {
        UserEntity user = userRepository.findByUserid(userid)
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
            user.setMarketingAgreedAt(Boolean.TRUE.equals(requestDTO.getMarketingOptIn()) ? LocalDateTime.now() : null);
        }

        // 2) [주소 정보 업데이트] 대표 이력서 찾아서 업데이트
        if (requestDTO.getAddress() != null) {
            resumeRepository.findByUserIdAndPrimaryTrue(user.getId())
                    .ifPresent(resume -> {
                        // 이력서에 프로필이 없으면 생성
                        if (resume.getProfile() == null) {
                            ResumeProfile newProfile = ResumeProfile.builder()
                                    .resume(resume)
                                    .build();
                            resume.updateProfile(newProfile); // Resume 엔티티의 편의 메소드 활용
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
    public void withdrawUser(String userid) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getDeletedAt() != null) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        user.setDeletedAt(LocalDateTime.now());
        // 필요 시 상태 코드 변경 로직 추가
        // user.setStatus("WITHDRAWN");

        log.info("회원 탈퇴 처리 완료: userid={}", userid);
    }
}