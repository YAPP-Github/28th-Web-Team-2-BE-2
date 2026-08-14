package com.example.demo.item.presentation.dto;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public record ItemQueryRequest(
        @Schema(description = "페이지 번호", example = "0", defaultValue = "0") Integer page,
        @Schema(description = "페이지 크기", example = "10", defaultValue = "10") Integer size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    public ItemQueryRequest {
        page = defaultValue(page, DEFAULT_PAGE);
        size = defaultValue(size, DEFAULT_SIZE);
        validate(page, size);
    }

    private static int defaultValue(final Integer value, final int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private static void validate(final int page, final int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new ApiException(
                    ErrorType.INVALID_PARAMETER_ERROR.description(),
                    ErrorType.INVALID_PARAMETER_ERROR,
                    HttpStatus.BAD_REQUEST);
        }
    }
}
