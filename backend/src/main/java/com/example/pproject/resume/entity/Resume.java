package com.example.pproject.resume.entity;

import com.example.pproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UserEntity와 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private UserEntity user;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    // AI 요약
    @Column(columnDefinition = "TEXT")
    private String summary;

    // 임베딩 벡터
    @ElementCollection
    private List<Double> embedding;

    // 첨부파일 리스트 (ResumeAttachment 타입 사용)
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeAttachment> attachments = new ArrayList<>();

    public Resume(UserEntity user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
    }

    // 편의 메서드
    public void addAttachment(ResumeAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setResume(this);
    }
}