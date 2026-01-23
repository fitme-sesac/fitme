package com.example.pproject.report.exception;

public class InvalidReportStatusException extends ReportException {

    private static final long serialVersionUID = 1L;

    public InvalidReportStatusException(String message) {
        super(message);
    }
}