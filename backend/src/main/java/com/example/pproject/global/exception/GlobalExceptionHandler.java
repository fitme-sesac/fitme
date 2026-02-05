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
     * [추가] IllegalArgumentException 처리
     * 
     * 언제 발생하나요?
     * - 서비스에서 throw new IllegalArgumentException("에러 메시지")를 던질 때
     * - 예: 중복 광고 캠페인 생성 시도, 존재하지 않는 ID 조회 등
     * 
     * 왜 필요한가요?
     * - 이 핸들러가 없으면 500 Internal Server Error + "서버 오류가 발생했습니다"가 뜸
     * - 이 핸들러가 있으면 400 Bad Request + 실제 에러 메시지가 클라이언트에 전달됨
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException 발생 - Message: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("400", e.getMessage()));
    }

    /**
     * [추가] IllegalStateException 처리
     * - 논리적 오류나 상태 오류 시 400 반환
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException 발생 - Message: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("400", e.getMessage()));
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {
        log.warn("Validation Exception 발생");

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

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
     * 일반 예외 처리 (수정됨)
     * - 반환 타입을 ResponseEntity<ApiResponse<Void>>로 변경하여 컴파일 에러 해결
     * - log.error(..., e)를 추가하여 스택 트레이스 출력
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected Exception 발생", e); // 이제 에러 상세 내용이 콘솔에 보입니다!

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