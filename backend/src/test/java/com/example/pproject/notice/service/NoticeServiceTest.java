package com.example.pproject.notice.service;

import com.example.pproject.notice.dto.NoticeCreateRequest;
import com.example.pproject.notice.dto.NoticeResponse;
import com.example.pproject.notice.entity.Notice;
import com.example.pproject.notice.entity.Notice.NoticeType;
import com.example.pproject.notice.repository.NoticeDeliveryRepository;
import com.example.pproject.notice.repository.NoticeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

// @ExtendWith(MockitoExtension.class): "스프링 말고, Mockito라는 가짜 만들기 도구를 쓸게"
@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock // 가짜 레포지토리 1
    private NoticeRepository noticeRepository;

    @Mock // 가짜 레포지토리 2 (발송용)
    private NoticeDeliveryRepository noticeDeliveryRepository;

    @InjectMocks // 가짜들을 진짜 서비스에 주입(끼워넣기)해줘
    private NoticeService noticeService;

    @Test
    @DisplayName("공지사항을 생성하면 저장된 결과가 반환된다")
    void createNotice() {
        // 1. [Given] 준비
        // 요청 데이터 만들기
        NoticeCreateRequest request = NoticeCreateRequest.builder()
                .title("테스트 제목")
                .body("테스트 본문")
                .isPublic(true)
                .noticeType(NoticeType.OPS)
                .build();

        // DB에 저장된 척 할 가짜 엔티티 만들기
        Notice fakeNotice = request.toEntity();

        // "레포지토리야, save()가 호출되면 저 가짜 엔티티를 리턴해줘" 라고 교육시킴
        given(noticeRepository.save(any(Notice.class))).willReturn(fakeNotice);

        // 2. [When] 실행
        NoticeResponse response = noticeService.createNotice(request, 1L);

        // 3. [Then] 검증
        assertThat(response.getTitle()).isEqualTo("테스트 제목"); // 제목이 똑같이 나왔나?

        // verify: 실제로 save 메서드가 한 번이라도 불렸는지 감시
        verify(noticeRepository).save(any(Notice.class));
    }

    @Test
    @DisplayName("존재하지 않는 공지사항을 조회하면 에러가 터져야 한다")
    void getNotice_Fail() {
        // 1. [Given]
        // "1번 아이디로 찾으면 빈통(Empty)을 줘라" (데이터 없음)
        given(noticeRepository.findById(1L)).willReturn(Optional.empty());

        // 2. [When & Then] 실행했을 때 예외가 발생하는지 확인
        assertThatThrownBy(() -> noticeService.getNoticePublic(1L))
                .isInstanceOf(IllegalArgumentException.class) // 에러 종류 확인
                .hasMessageContaining("찾을 수 없거나"); // 에러 메시지 확인
    }
}