package com.example.pproject.resume.entity;

import com.example.pproject.Constant.ScanStatus;
import com.example.pproject.common.entity.BaseSoftDeleteEntity; // 혹은 BaseTimeEntity 사용 시 변경
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@Table(name = "resume_attachment")
public class ResumeAttachment extends BaseSoftDeleteEntity {

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

    @Column(name = "ai_description", columnDefinition = "TEXT")
    private String aiDescription;

    @Builder
    public ResumeAttachment(Resume resume, String fileUrl, String fileName, String mimeType, Long fileSize, ScanStatus scanStatus, String aiDescription) {
        this.resume = resume;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.scanStatus = (scanStatus != null) ? scanStatus : ScanStatus.PENDING;
        this.aiDescription = aiDescription;
        this.copyrightOk = false;
    }
}