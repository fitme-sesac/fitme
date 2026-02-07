package com.example.pproject.admin.exception;

public class AdminMemberException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AdminMemberException(String message) {
        super(message);
    }

    public AdminMemberException(String message, Throwable cause) {
        super(message, cause);
    }
}