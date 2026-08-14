package com.example.demo.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.auth.application.port.KakaoTokenClient;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.junit.jupiter.api.Test;

class KakaoTestRedirectUseCaseTest {

    private final KakaoTokenClient kakaoTokenClient = mock(KakaoTokenClient.class);
    private final KakaoTestRedirectUseCase useCase = new KakaoTestRedirectUseCase(kakaoTokenClient);

    @Test
    void authorization_code를_외부_Kakao_client에_전달하고_idToken을_반환한다() {
        when(kakaoTokenClient.exchangeIdToken("authorization-code")).thenReturn("id-token");

        assertThat(useCase.execute("authorization-code")).isEqualTo("id-token");
    }

    @Test
    void 빈_authorization_code는_외부_호출_전에_400_오류를_던진다() {
        assertThatThrownBy(() -> useCase.execute(" "))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_PARAMETER_ERROR));

        verifyNoInteractions(kakaoTokenClient);
    }
}
