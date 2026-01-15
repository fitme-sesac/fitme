package com.example.pproject.resume.dto;

import com.example.pproject.Constant.ResumeField;
import com.example.pproject.Constant.ResumeStatus;
import com.example.pproject.Constant.SummaryStatus;
import com.example.pproject.resume.entity.Resume;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class ResumeResponseDto {

    private Long resumeId;
    private String title;
    private String tagline;
    private String content;

    // 프론트에 보여줄 AI 요약 결과
    private String summary;
    private SummaryStatus summaryStatus;

    private ResumeField field;
    private ResumeStatus status;
    private LocalDateTime updatedAt;

    // 상세 정보 리스트 (프로젝트, 첨부파일 등)
    private List<AttachmentResponseDto> attachments;

    /**
     * Create a ResumeResponseDto from a Resume entity.
     *
     * Maps the resume's id, title, tagline, content, AI summary and status, domain fields (field, status, updatedAt),
     * and converts attachments to a list of AttachmentResponseDto when the source attachments are non-null.
     *
     * @param resume the source Resume entity to convert; if resume.getAttachments() is null, the DTO's attachments remain null
     */
    public ResumeResponseDto(Resume resume) {
        this.resumeId = resume.getId();
        this.title = resume.getTitle();
        this.tagline = resume.getTagline();
        this.content = resume.getContent();

        // AI 결과 매핑
        this.summary = resume.getSummary();
        this.summaryStatus = resume.getSummaryStatus();

        this.field = resume.getField();
        this.status = resume.getStatus();
        this.updatedAt = resume.getUpdatedAt();

        if (resume.getAttachments() != null) {
            this.attachments = resume.getAttachments().stream()
                    .map(AttachmentResponseDto::new)
                    .collect(Collectors.toList());
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class AttachmentResponseDto {
        private Long id;
        private String fileUrl;
        private String fileName;
        private String aiDescription;

        /**
         * Creates an AttachmentResponseDto populated from the given ResumeAttachment entity.
         *
         * @param entity the source ResumeAttachment whose id, fileUrl, fileName, and aiDescription are copied into this DTO
         */
        public AttachmentResponseDto(com.example.pproject.resume.entity.ResumeAttachment entity) {
            this.id = entity.getId();
            this.fileUrl = entity.getFileUrl();
            this.fileName = entity.getFileName();
            this.aiDescription = entity.getAiDescription();
        }
    }
}