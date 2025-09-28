package com.vibe.jobs.auth.application;

import org.springframework.http.HttpStatus;

public class AuthFlowException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public AuthFlowException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
