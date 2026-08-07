package com.example.demo.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final String errorMessage;
    private final ErrorType errorType;
    private final HttpStatus httpStatus;

    public ApiException(
            final String errorMessage,
            final ErrorType errorType,
            final HttpStatus httpStatus) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.httpStatus = httpStatus;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public ErrorType errorType() {
        return errorType;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
