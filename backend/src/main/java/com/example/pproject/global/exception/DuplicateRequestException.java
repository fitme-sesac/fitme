package com.example.pproject.global.exception;

/**
 * 중복 요청 발생 시 던지는 예외.
 * <p>
 * Redis 분산 락 획득 실패 시 발생합니다.
 * HTTP 429 Too Many Requests로 응답됩니다.
 * </p>
 */
public class DuplicateRequestException extends RuntimeException {

    public DuplicateRequestException(String message) {
        super(message);
    }
}
