package com.example.pproject.resume.dto;

import com.example.pproject.resume.entity.Resume;
import com.example.pproject.resume.entity.ResumeAttachment;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class ResumeResponseDto {

    private Long id;
    private String title;
    private String content;
    private String summary;

    private Integer userId;
    private String userName;

    private List<String> fileNames;

    public ResumeResponseDto(Resume resume) {
        this.id = resume.getId();
        this.title = resume.getTitle();
        this.content = resume.getContent();
        this.summary = resume.getSummary();

        // 유저 정보 매핑
        if (resume.getUser() != null) {
            this.userId = resume.getUser().getId();
            this.userName = resume.getUser().getUsername();
        }

        // 첨부파일 매핑
        if (resume.getAttachments() != null) {
            this.fileNames = resume.getAttachments().stream()
                    .map(ResumeAttachment::getFileName)
                    .collect(Collectors.toList());
        } else {
            this.fileNames = Collections.emptyList();
        }
    }
}