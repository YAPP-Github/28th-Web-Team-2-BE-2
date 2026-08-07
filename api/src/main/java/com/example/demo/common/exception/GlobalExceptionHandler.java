package com.example.demo.common.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(final ApiException exception) {
        return ResponseEntity.status(exception.httpStatus())
                .body(new ApiErrorResponse(exception.errorType().name(), exception.errorMessage()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiErrorResponse> handleFeignException(final FeignException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse(
                        ErrorType.EXTERNAL_API_ERROR.name(),
                        ErrorType.EXTERNAL_API_ERROR.description()));
    }
}
