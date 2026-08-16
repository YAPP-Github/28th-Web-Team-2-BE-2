package com.example.demo.external.kakao.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.junit.jupiter.api.Test;

class KakaoMapClientConfigurationTest {

    @Test
    void Kakao_REST_API_Key가_비어있으면_설정에_실패한다() {
        assertThatThrownBy(() -> new KakaoMapClientConfiguration().requestInterceptor(" "))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.CONFIGURATION_ERROR));
    }
}
