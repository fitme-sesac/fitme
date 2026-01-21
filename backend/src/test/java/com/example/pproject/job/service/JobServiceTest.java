package com.example.pproject.job.service;

import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.entity.EmployerMemberEntity;
import com.example.pproject.employer.repository.EmployerMemberRepository;
import com.example.pproject.employer.repository.EmployerRepository;
import com.example.pproject.job.dto.JobCreateDTO;
import com.example.pproject.job.dto.JobDTO;
import com.example.pproject.job.entity.JobEntity;
import com.example.pproject.job.repository.JobRepository;
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
class JobServiceTest {

    @InjectMocks
    private JobService jobService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private EmployerRepository employerRepository;

    @Mock
    private EmployerMemberRepository employerMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("채용공고 생성 서비스 로직 성공 테스트")
    void createJob_Success() {
        // 1. Given (준비)
        String userid = "testuser";
        Long memberId = 1L;
        Long employerId = 10L;

        // 요청 데이터(DTO) 준비
        JobCreateDTO request = new JobCreateDTO();
        request.setTitle("백엔드 개발자 구인");
        request.setDescription("자바 스프링 개발자를 구합니다.");
        request.setLocation("서울 강남구");
        request.setSalaryText("면접 후 결정");
        request.setStack("Java, Spring Boot");

        // 가짜 유저(UserEntity) 준비
        UserEntity mockUser = UserEntity.builder()
                .id(memberId.intValue())
                .userid(userid)
                .build();

        // 가짜 기업(EmployerEntity) 준비
        EmployerEntity mockEmployer = EmployerEntity.builder()
                .name("테스트 기업")
                .status("ACTIVE")
                .build();
        ReflectionTestUtils.setField(mockEmployer, "id", employerId);

        // 가짜 멤버십(EmployerMemberEntity) 준비
        EmployerMemberEntity mockMembership = EmployerMemberEntity.builder()
                .employerId(employerId)
                .memberId(memberId)
                .active(true)
                .build();

        // 저장될 JobEntity 준비
        JobEntity savedJob = JobEntity.builder()
                .employerId(employerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .status("DRAFT")
                .build();
        ReflectionTestUtils.setField(savedJob, "id", 100L);

        // [핵심] 가짜 행동 정의 (Mocking)
        given(userRepository.findByUserid(userid)).willReturn(Optional.of(mockUser));
        given(employerMemberRepository.findFirstByMemberIdAndActiveTrue(memberId)).willReturn(Optional.of(mockMembership));
        given(employerRepository.findById(employerId)).willReturn(Optional.of(mockEmployer));
        given(jobRepository.save(any(JobEntity.class))).willReturn(savedJob);

        // 2. When (실행)
        JobDTO result = jobService.createJob(userid, request);

        // 3. Then (검증)
        assertThat(result.getTitle()).isEqualTo("백엔드 개발자 구인");
        assertThat(result.getCompanyName()).isEqualTo("테스트 기업");

        verify(jobRepository).save(any(JobEntity.class));
    }
}
