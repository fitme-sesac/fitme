package com.example.pproject.resume.entity;

import com.example.pproject.common.entity.BaseTimeEntity;
import com.example.pproject.Constant.ScanStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "resume_attachment", indexes = @Index(name = "idx_resume_attachment_resume_id", columnList = "resume_id"))
public class ResumeAttachment extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "mime_type", length = 80)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    private ScanStatus scanStatus;

    @Column(name = "copyright_ok", nullable = false)
    @ColumnDefault("false")
    private boolean copyrightOk;

    @Lob @Column(name = "ai_description")
    private String aiDescription;

    public void setResume(Resume resume) { this.resume = resume; }
}