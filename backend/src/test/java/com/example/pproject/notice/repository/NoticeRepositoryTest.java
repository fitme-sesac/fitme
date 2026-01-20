package com.example.pproject.notice.repository;

import com.example.pproject.notice.entity.Notice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource; // 👈 import 추가

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NoticeRepositoryTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Test
    @DisplayName("정책 목록 조회: POLICY, TERMS, PRIVACY 타입만 조회되어야 한다.")
    void findPolicies_Filter() {
        // given
        Notice policy = createNotice("정책1", Notice.NoticeType.POLICY);
        Notice terms = createNotice("약관1", Notice.NoticeType.TERMS);
        Notice privacy = createNotice("개인정보1", Notice.NoticeType.PRIVACY);
        Notice ops = createNotice("운영공지1", Notice.NoticeType.OPS); // 이건 조회되면 안 됨

        noticeRepository.saveAll(List.of(policy, terms, privacy, ops));

        // when
        Page<Notice> result = noticeRepository.findPolicies(PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(3); // OPS 제외 3개
        assertThat(result.getContent())
                .extracting("noticeType")
                .contains(
                        Notice.NoticeType.POLICY,
                        Notice.NoticeType.TERMS,
                        Notice.NoticeType.PRIVACY
                );
        assertThat(result.getContent())
                .extracting("noticeType")
                .doesNotContain(Notice.NoticeType.OPS);
    }

    private Notice createNotice(String title, Notice.NoticeType type) {
        return Notice.builder()
                .title(title)
                .body("내용")
                .noticeType(type)
                .status(Notice.NoticeStatus.ACTIVE)
                .isPublic(true)
                .isImportant(false)
                .build();
    }
}