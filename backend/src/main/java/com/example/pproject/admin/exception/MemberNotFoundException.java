package com.example.pproject.admin.exception;

public class MemberNotFoundException extends AdminMemberException {

    private static final long serialVersionUID = 1L;

    public MemberNotFoundException(String message) {
        super(message);
    }
}