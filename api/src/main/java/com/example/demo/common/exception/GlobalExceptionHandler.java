package com.example.demo.common.exception;

import com.example.demo.common.presentation.ApiV1Response;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(
            final ApiException exception,
            final HttpServletRequest request) {
        if (isV1Request(request)) {
            return ResponseEntity.status(exception.httpStatus())
                    .body(new ApiV1Response<>(
                            exception.errorType().name(),
                            exception.errorMessage(),
                            null));
        }
        return ResponseEntity.status(exception.httpStatus())
                .body(new ApiErrorResponse(exception.errorType().name(), exception.errorMessage()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<?> handleFeignException(
            final FeignException exception,
            final HttpServletRequest request) {
        if (isV1Request(request)) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ApiV1Response<>(
                            ErrorType.EXTERNAL_API_ERROR.name(),
                            ErrorType.EXTERNAL_API_ERROR.description(),
                            null));
        }
        return handleLegacyFeignException();
    }

    public ResponseEntity<ApiErrorResponse> handleFeignException(final FeignException exception) {
        return handleLegacyFeignException();
    }

    private ResponseEntity<ApiErrorResponse> handleLegacyFeignException() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse(
                        ErrorType.EXTERNAL_API_ERROR.name(),
                        ErrorType.EXTERNAL_API_ERROR.description()));
    }

    @ExceptionHandler({
        BindException.class,
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<?> handleInvalidParameter(
            final Exception exception,
            final HttpServletRequest request) {
        if (isV1Request(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiV1Response<>(
                            ErrorType.INVALID_PARAMETER_ERROR.name(),
                            ErrorType.INVALID_PARAMETER_ERROR.description(),
                            null));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        ErrorType.INVALID_PARAMETER_ERROR.name(),
                        ErrorType.INVALID_PARAMETER_ERROR.description()));
    }

    private boolean isV1Request(final HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/v1/");
    }
}
