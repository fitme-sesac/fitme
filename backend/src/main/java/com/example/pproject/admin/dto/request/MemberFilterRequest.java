package com.example.pproject.admin.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MemberFilterRequest {
    // 1. 식별자 필터
    private String member_id;         // 회원 ID (숫자 변환 필요)
    private String member_uid;        // 회원 UUID

    // 2. 기본 정보 필터
    private String name;              // 회원명
    private String login_id;          // 아이디
    private String email;             // 이메일
    private String phone;             // 전화번호

    // 3. 분류 필터
    private String role;              // CANDIDATE, EMPLOYER, MASTER
    private String status;            // ACTIVE, SUSPENDED, WITHDRAWN
    // DTO 파일 내부
    private String memberGrade; // 변수명이 memberGrade여야 getMemberGrade()가 작동함

    // 4. 관리 지표
    private String penalty_point;     // 입력값 이상 (>=) 검색
    private Boolean isPaidMember;     // 결제 회원 여부 (true/false)

    // 5. 날짜 필터 (YYYY-MM-DD)
    private String created_at;        // 가입일
    private String deleted_at;        // 탈퇴일
    private String last_login_at;     // 최종 접속일

    // 6. 기간 조회 (예: "2024-01-01~2024-12-31")
    private String dateRange;
}