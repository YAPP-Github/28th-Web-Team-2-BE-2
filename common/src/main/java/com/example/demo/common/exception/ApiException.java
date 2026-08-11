package com.example.demo.common.exception;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Getter
@Accessors(fluent = true)
public class  ApiException extends RuntimeException {

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

    public static ApiException invalidParameter() {
        return new ApiException(
                ErrorType.INVALID_PARAMETER_ERROR.description(),
                ErrorType.INVALID_PARAMETER_ERROR,
                HttpStatus.BAD_REQUEST);
    }
}
