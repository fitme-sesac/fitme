package com.example.pproject.application.entity;

import com.example.pproject.Constant.InterviewMethod;
import com.example.pproject.Constant.InterviewStage;
import com.example.pproject.Constant.InterviewStatus;
import com.example.pproject.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 면접 일정 Entity
 * ERD: interview_schedule 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interview_schedule")
public class InterviewSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private InterviewStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private InterviewMethod method;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "meeting_url", columnDefinition = "TEXT")
    private String meetingUrl;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InterviewStatus status;

    @Column(name = "created_by_member_id")
    private Long createdByMemberId;

    @Builder
    public InterviewSchedule(JobApplication application, InterviewStage stage, InterviewMethod method,
                             String location, String meetingUrl, LocalDateTime startAt, LocalDateTime endAt,
                             Long createdByMemberId) {
        this.application = application;
        this.stage = stage;
        this.method = method;
        this.location = location;
        this.meetingUrl = meetingUrl;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = InterviewStatus.PROPOSED;
        this.createdByMemberId = createdByMemberId;
    }

    /**
     * 면접 확정 (지원자 수락)
     */
    public void confirm() {
        this.status = InterviewStatus.CONFIRMED;
    }

    /**
     * 면접 취소
     */
    public void cancel() {
        this.status = InterviewStatus.CANCELED;
    }

    /**
     * 면접 완료
     */
    public void complete() {
        this.status = InterviewStatus.DONE;
    }

    /**
     * 일정 변경
     */
    public void reschedule(LocalDateTime newStartAt, LocalDateTime newEndAt, String newLocation, String newMeetingUrl) {
        this.startAt = newStartAt;
        this.endAt = newEndAt;
        if (newLocation != null) this.location = newLocation;
        if (newMeetingUrl != null) this.meetingUrl = newMeetingUrl;
        this.status = InterviewStatus.PROPOSED; // 재제안 상태로 변경
    }
}
