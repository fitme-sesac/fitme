package com.example.pproject.global.exception;

import com.example.pproject.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business Exception 발생 - Code: {}, Message: {}", e.getErrorCode().getCode(), e.getMessage());

        HttpStatus status = getHttpStatus(e.getErrorCode());

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {
        log.warn("Validation Exception 발생");

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError(errors));
    }

    /**
     * 404 예외 처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NoHandlerFoundException e) {
        log.warn("Not Found Exception 발생 - URL: {}", e.getRequestURL());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("404", "요청한 엔드포인트를 찾을 수 없습니다"));
    }

    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        log.error("Unexpected Exception 발생", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    /**
     * ErrorCode에 따른 HttpStatus 결정
     */
    private HttpStatus getHttpStatus(ErrorCode errorCode) {
        if (errorCode.getCode().startsWith("400")) {
            return HttpStatus.BAD_REQUEST;
        } else if (errorCode.getCode().startsWith("401")) {
            return HttpStatus.UNAUTHORIZED;
        } else if (errorCode.getCode().startsWith("403")) {
            return HttpStatus.FORBIDDEN;
        } else if (errorCode.getCode().startsWith("404")) {
            return HttpStatus.NOT_FOUND;
        } else if (errorCode.getCode().startsWith("409")) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}