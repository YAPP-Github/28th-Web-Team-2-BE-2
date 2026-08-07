package com.example.demo.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionTest {

    @Test
    void 공통_예외는_메시지_유형_HTTP_상태를_보유한다() {
        final ApiException exception = new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY);

        assertThat(exception.errorMessage()).isEqualTo(ErrorType.EXTERNAL_API_ERROR.description());
        assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
        assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
