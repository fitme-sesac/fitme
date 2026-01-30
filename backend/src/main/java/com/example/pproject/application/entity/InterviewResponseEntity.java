package com.example.pproject.application.entity;

import com.example.pproject.Constant.InterviewResponseType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 면접 응답 Entity
 * ERD: interview_response 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interview_response")
public class InterviewResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewSchedule interview;

    @Enumerated(EnumType.STRING)
    @Column(name = "response", nullable = false, length = 20)
    private InterviewResponseType response;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "responded_at", nullable = false)
    private LocalDateTime respondedAt;

    @Builder
    public InterviewResponseEntity(InterviewSchedule interview, InterviewResponseType response, String message) {
        this.interview = interview;
        this.response = response;
        this.message = message;
        this.respondedAt = LocalDateTime.now();
    }
}
