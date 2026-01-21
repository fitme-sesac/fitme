package com.example.pproject.resume.service;

import com.example.pproject.Constant.ResumeField;
import com.example.pproject.application.repository.JobApplicationRepository;
import com.example.pproject.resume.dto.ResumeRequest;
import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.repository.ResumeRepository;
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

@ExtendWith(MockitoExtension.class) // 가짜 객체(Mock) 사용 선언
class ResumeServiceTest {

    @InjectMocks // 가짜 의존성들을 주입받을 '진짜' 서비스
    private ResumeService resumeService;

    @Mock // 서비스가 필요로 하는 레포지토리 (가짜)
    private ResumeRepository resumeRepository;

    @Mock // 서비스가 필요로 하는 유저 레포지토리 (가짜)
    private UserRepository userRepository;

    @Mock // 삭제 로직 등에서 쓰이는 의존성 (생성 테스트엔 안 쓰이지만 주입 필요)
    private JobApplicationRepository jobApplicationRepository;

    @Test
    @DisplayName("이력서 생성 서비스 로직 성공 테스트")
    void createResume_Success() {
        // 1. Given (준비)
        Integer userId = 1;

        // 요청 데이터(DTO) 준비
        ResumeRequest request = new ResumeRequest();
        request.setTitle("신입 백엔드 개발자 이력서");
        request.setField(ResumeField.RESUME); // 이력서 타입
        request.setContent("열심히 하겠습니다.");
        request.setPrimary(true); // 대표 이력서 설정

        // 가짜 유저(UserEntity) 준비
        UserEntity mockUser = UserEntity.builder()
                .id(userId)
                .username("테스트유저")
                .email("test@example.com")
                .build();

        // 레포지토리가 저장 후 반환할 가짜 이력서(Resume) 준비
        Resume savedResume = Resume.builder()
                .user(mockUser)
                .title(request.getTitle())
                .field(request.getField())
                .build();

        // ID는 DB가 자동생성하므로, 테스트에선 강제로 주입해줌 (Reflection 사용)
        ReflectionTestUtils.setField(savedResume, "id", 100L);

        // [핵심] 가짜 행동 정의 (Mocking)
        // "유저 조회하면 mockUser를 리턴해라"
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        // "이력서 저장하면 savedResume(ID:100)을 리턴해라"
        given(resumeRepository.save(any(Resume.class))).willReturn(savedResume);

        // 2. When (실행)
        Long resultId = resumeService.createResume(request, userId);

        // 3. Then (검증)
        // 반환된 ID가 100인지 확인
        assertThat(resultId).isEqualTo(100L);

        // 실제로 resumeRepository.save()가 호출되었는지 확인
        verify(resumeRepository).save(any(Resume.class));
    }
}