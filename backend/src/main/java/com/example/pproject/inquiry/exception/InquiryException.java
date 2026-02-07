package com.example.pproject.inquiry.exception;

public class InquiryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InquiryException(String message) {
        super(message);
    }

    public InquiryException(String message, Throwable cause) {
        super(message, cause);
    }
}