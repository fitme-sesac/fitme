package com.example.pproject.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCreateRequest {
    
    @NotBlank(message = "댓글 내용을 입력해주세요")
    private String content;
}
