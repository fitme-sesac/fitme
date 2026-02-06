package com.example.pproject.inquiry.handler;

import com.example.pproject.inquiry.exception.*;
import com.example.pproject.inquiry.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.pproject.inquiry")
@Slf4j
public class InquiryExceptionHandler {

    /**
     * InquiryNotFoundException 처리
     */
    @ExceptionHandler(InquiryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInquiryNotFoundException(
            InquiryNotFoundException e) {
        log.warn("InquiryNotFoundException: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.NOT_FOUND.value())
                .message(e.getMessage())
                .status("NOT_FOUND")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * InvalidInquiryStatusException 처리
     */
    @ExceptionHandler(InvalidInquiryStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInquiryStatusException(
            InvalidInquiryStatusException e) {
        log.warn("InvalidInquiryStatusException: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(e.getMessage())
                .status("INVALID_STATUS")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * InquiryException 기본 처리
     */
    @ExceptionHandler(InquiryException.class)
    public ResponseEntity<ErrorResponse> handleInquiryException(
            InquiryException e) {
        log.error("InquiryException: {}", e.getMessage(), e);

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(e.getMessage())
                .status("INTERNAL_ERROR")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Validation 오류 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e) {
        log.warn("Validation Error: {}", e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    fieldErrors.put(fieldName, errorMessage);
                });

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("유효성 검사 실패")
                .status("VALIDATION_ERROR")
                .errors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}