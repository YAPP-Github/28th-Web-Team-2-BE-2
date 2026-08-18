package com.example.demo.item.application.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.junit.jupiter.api.Test;

class BatchItemFailureTest {

    @Test
    void 유효하지_않은_식별자는_공통_API_예외로_거부한다() {
        assertThatThrownBy(() -> new BatchItemFailure(
                0L,
                2,
                new IllegalStateException("cause")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_PARAMETER_ERROR);
                    assertThat(exception.errorMessage())
                            .isEqualTo(ErrorType.INVALID_PARAMETER_ERROR.description());
                });
    }
}
