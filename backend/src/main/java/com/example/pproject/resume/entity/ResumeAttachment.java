package com.example.pproject.resume.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "resume_attachment")
public class ResumeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    // 파일 내용도 AI가 읽고 요약할 수 있도록 저장하는 필드
    @Column(name = "ai_description", columnDefinition = "TEXT")
    private String aiDescription;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Create a ResumeAttachment with the given file URL and file name.
     *
     * @param fileUrl  the URL or storage path of the attachment file
     * @param fileName the display or original name of the file
     */
    public ResumeAttachment(String fileUrl, String fileName) {
        this.fileUrl = fileUrl;
        this.fileName = fileName;
    }
}