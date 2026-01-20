package com.example.pproject.notice.service;

import org.junit.jupiter.api.Test;
import com.example.pproject.global.exception.BusinessException;
import com.example.pproject.global.exception.ErrorCode;
import com.example.pproject.notice.dto.NoticeCreateRequest;
import com.example.pproject.notice.dto.NoticeResponse;
import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.repository.NoticeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @InjectMocks
    private NoticeService noticeService;

    @Mock
    private NoticeRepository noticeRepository;

    @Test
    @DisplayName("정책 등록 성공: 중복된 활성 정책이 없으면 등록된다.")
    void createPolicy_Success() {
        // given
        NoticeCreateRequest request = NoticeCreateRequest.builder()
                .title("이용약관 v1.0")
                .body("내용")
                .noticeType("TERMS") // 정책 타입
                .build();

        given(noticeRepository.existsByNoticeTypeAndStatus(any(), any()))
                .willReturn(false); // 중복 없음 설정

        given(noticeRepository.save(any(Notice.class)))
                .willAnswer(invocation -> {
                    Notice notice = invocation.getArgument(0);
                    notice.setId(1L); // ID 부여 시뮬레이션
                    return notice;
                });

        // when
        NoticeResponse response = noticeService.createPolicy(request);

        // then
        assertThat(response.getTitle()).isEqualTo("이용약관 v1.0");
        assertThat(response.getNoticeType()).isEqualTo("TERMS");
        verify(noticeRepository, times(1)).save(any(Notice.class));
    }

    @Test
    @DisplayName("정책 등록 실패: 이미 활성화된 같은 타입의 정책이 있으면 예외 발생")
    void createPolicy_Fail_Duplicate() {
        // given
        NoticeCreateRequest request = NoticeCreateRequest.builder()
                .title("개인정보처리방침 v2")
                .body("내용")
                .noticeType("PRIVACY")
                .build();

        given(noticeRepository.existsByNoticeTypeAndStatus(
                Notice.NoticeType.PRIVACY, Notice.NoticeStatus.ACTIVE))
                .willReturn(true); // 이미 존재한다고 가정

        // when & then
        assertThatThrownBy(() -> noticeService.createPolicy(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 활성화된 해당 타입의 정책이 존재합니다");
    }

    @Test
    @DisplayName("정책 등록 실패: OPS(운영공지) 타입으로 정책 등록 시도 시 예외 발생")
    void createPolicy_Fail_InvalidType() {
        // given
        NoticeCreateRequest request = NoticeCreateRequest.builder()
                .title("잘못된 요청")
                .body("내용")
                .noticeType("OPS") // 운영 공지
                .build();

        // when & then
        assertThatThrownBy(() -> noticeService.createPolicy(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("운영 공지(OPS)는 정책으로 등록할 수 없습니다");
    }

    @Test
    @DisplayName("공지 삭제 로직 검증: 30일 유예 기간이 정확히 설정되는가?")
    void notice_SoftDelete_Logic() {
        // given
        Notice notice = Notice.builder()
                .id(1L)
                .title("삭제할 공지")
                .status(Notice.NoticeStatus.ACTIVE)
                .build();

        // when
        notice.delete(); // Entity 내부 메서드 호출

        // then
        assertThat(notice.getStatus()).isEqualTo(Notice.NoticeStatus.PENDING_DELETE);
        assertThat(notice.getDeletedAt()).isNotNull();

        // 30일 뒤인지 확인 (오차 범위 1분)
        assertThat(notice.getPurgeAfter()).isCloseTo(
                LocalDateTime.now().plusDays(30),
                org.assertj.core.api.Assertions.within(1, ChronoUnit.MINUTES)
        );
    }
}