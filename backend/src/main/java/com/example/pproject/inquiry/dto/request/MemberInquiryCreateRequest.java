package com.example.pproject.inquiry.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberInquiryCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @JsonProperty("body") // JSON의 "body"를 자바의 content로 매핑
    private String content;

    // 카테고리가 명세에 없다면 "GENERAL" 등 기본값 처리하거나 null 허용
    private String category;
}