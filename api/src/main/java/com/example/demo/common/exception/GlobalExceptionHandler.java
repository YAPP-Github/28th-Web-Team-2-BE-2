package com.example.demo.common.exception;

import com.example.demo.common.presentation.ApiResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(
            final ApiException exception,
            final HttpServletRequest request) {
        if (isV1Request(request)) {
            return ResponseEntity.status(exception.httpStatus())
                    .body(new ApiResponse<>(
                            exception.errorType().name(),
                            exception.errorMessage(),
                            null));
        }
        return ResponseEntity.status(exception.httpStatus())
                .body(new ApiErrorResponse(exception.errorType().name(), exception.errorMessage()));
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<?> handleAuthenticationRequired(final HttpServletRequest request) {
        return handleApiException(
                new ApiException(
                        ErrorType.UNAUTHORIZED.description(),
                        ErrorType.UNAUTHORIZED,
                        HttpStatus.UNAUTHORIZED),
                request);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<?> handleFeignException(
            final FeignException exception,
            final HttpServletRequest request) {
        if (isV1Request(request)) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ApiResponse<>(
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
        MissingServletRequestPartException.class,
        MethodArgumentTypeMismatchException.class,
        HandlerMethodValidationException.class
    })
    public ResponseEntity<?> handleInvalidParameter(
            final Exception exception,
            final HttpServletRequest request) {
        if (isV1Request(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            ErrorType.INVALID_PARAMETER_ERROR.name(),
                            ErrorType.INVALID_PARAMETER_ERROR.description(),
                            null));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        ErrorType.INVALID_PARAMETER_ERROR.name(),
                        ErrorType.INVALID_PARAMETER_ERROR.description()));
    }

    /**
     * 서블릿 multipart 상한을 넘긴 업로드.
     *
     * <p>도메인 상한(5MB)은 {@code ImageSize}가 잡지만, 그보다 큰 요청은 컨테이너가 본문을 다 읽기
     * 전에 끊어 이 예외가 된다. 처리하지 않으면 클라이언트가 400 {@code IMAGE_TOO_LARGE} 대신
     * 500과 공통 envelope이 아닌 body를 받는다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceeded(final HttpServletRequest request) {
        return handleApiException(
                new ApiException(
                        ErrorType.IMAGE_TOO_LARGE.description(),
                        ErrorType.IMAGE_TOO_LARGE,
                        HttpStatus.BAD_REQUEST),
                request);
    }

    /**
     * 도메인이 알린 이미지 입력 오류.
     *
     * <p>도메인은 HTTP 상태 코드를 모른다({@code docs/ARCHITECTURE.md} §8). 매핑을 여기서 한다.
     */
    @ExceptionHandler(ImageValidationException.class)
    public ResponseEntity<?> handleImageValidation(
            final ImageValidationException exception, final HttpServletRequest request) {
        return handleApiException(
                new ApiException(
                        exception.errorType().description(),
                        exception.errorType(),
                        HttpStatus.BAD_REQUEST),
                request);
    }

    private boolean isV1Request(final HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/v1/");
    }
}
