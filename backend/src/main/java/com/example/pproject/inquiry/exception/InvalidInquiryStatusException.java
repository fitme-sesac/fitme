package com.example.pproject.inquiry.exception;

public class InvalidInquiryStatusException extends InquiryException {

    private static final long serialVersionUID = 1L;

    public InvalidInquiryStatusException(String message) {
        super(message);
    }
}