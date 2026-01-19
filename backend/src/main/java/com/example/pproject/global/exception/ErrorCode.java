package com.example.pproject.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT("400", "입력값이 유효하지 않습니다"),
    NOT_FOUND("404", "요청한 리소스를 찾을 수 없습니다"),
    DUPLICATE_RESOURCE("409", "이미 존재하는 리소스입니다"),
    UNAUTHORIZED("401", "인증되지 않은 요청입니다"),
    FORBIDDEN("403", "권한이 없습니다"),
    INTERNAL_SERVER_ERROR("500", "서버 오류가 발생했습니다"),
    INVALID_INPUT_VALUE("400", "잘못된 입력값입니다."),


    // FAQ
    FAQ_NOT_FOUND("404_FAQ", "FAQ를 찾을 수 없습니다"),
    DUPLICATE_FAQ_QUESTION("409_FAQ", "이미 존재하는 FAQ 질문입니다");

    private final String code;
    private final String message;
}
