package com.example.pproject.employer.service;

import com.example.pproject.employer.dto.EmployerProfileDTO;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmployerServiceTest {

    @InjectMocks
    private EmployerService employerService;

    @Mock
    private EmployerRepository employerRepository;

    @Mock
    private EmployerMemberRepository employerMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("기업 프로필 생성 서비스 로직 성공 테스트")
    void createProfile_Success() {
        // 1. Given (준비)
        String userid = "testuser";
        Long memberId = 1L;

        // 요청 데이터(DTO) 준비
        EmployerProfileDTO request = EmployerProfileDTO.builder()
                .name("테스트 기업")
                .industry("IT")
                .location("서울")
                .contactEmail("contact@test.com")
                .build();

        // 가짜 유저(UserEntity) 준비
        UserEntity mockUser = UserEntity.builder()
                .id(memberId.intValue())
                .userid(userid)
                .username("테스트유저")
                .email("test@example.com")
                .build();

        // 레포지토리가 저장 후 반환할 가짜 기업(EmployerEntity) 준비
        EmployerEntity savedEmployer = EmployerEntity.builder()
                .name(request.getName())
                .industry(request.getIndustry())
                .location(request.getLocation())
                .contactEmail(request.getContactEmail())
                .build();
        
        // ID 주입
        ReflectionTestUtils.setField(savedEmployer, "id", 100L);
        ReflectionTestUtils.setField(savedEmployer, "employerUid", java.util.UUID.randomUUID());

        // [핵심] 가짜 행동 정의 (Mocking)
        given(userRepository.findByUserid(userid)).willReturn(Optional.of(mockUser));
        given(employerMemberRepository.existsByMemberIdAndActiveTrue(memberId)).willReturn(false);
        given(employerRepository.save(any(EmployerEntity.class))).willReturn(savedEmployer);
        given(employerMemberRepository.save(any(EmployerMemberEntity.class))).willReturn(EmployerMemberEntity.builder().build());

        // 2. When (실행)
        EmployerProfileDTO result = employerService.createProfile(userid, request);

        // 3. Then (검증)
        assertThat(result.getName()).isEqualTo("테스트 기업");
        assertThat(result.getRoleInCompany()).isEqualTo("OWNER");

        verify(employerRepository).save(any(EmployerEntity.class));
        verify(employerMemberRepository).save(any(EmployerMemberEntity.class));
    }
}
