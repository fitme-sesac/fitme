package com.example.pproject.talent.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 인재풀 상세 DTO (기업 회원 열람용)
 */
@Data
@Builder
public class TalentDetailDTO {
    private Long id;           // member_id
    private String name;
    private String title;
    private String summary;
    private String experience;
    private String location;
    private String education;
    private List<String> skills;
    private String salary;
    private int matchScore;
    private String lastUpdated;
    private boolean isNew;
    private String email;      // 기업 회원에게만 (열람권 등 적용 시)
    private String phone;      // 마스킹 또는 기업 회원만
}
