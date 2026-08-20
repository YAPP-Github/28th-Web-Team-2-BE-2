package com.example.demo.common.exception;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 이미지 입력이 규칙을 어겼을 때 도메인이 던지는 예외.
 *
 * <p>{@link ApiException}을 쓰지 않는 이유는 {@code docs/ARCHITECTURE.md} §8이다 — Domain은 HTTP
 * 상태 코드에 의존하지 않는다. 상태 코드 매핑은 {@code GlobalExceptionHandler}가 한다.
 */
@Getter
@Accessors(fluent = true)
public class ImageValidationException extends RuntimeException {

    private final ErrorType errorType;

    public ImageValidationException(final ErrorType errorType) {
        super(errorType.description());
        this.errorType = errorType;
    }
}
