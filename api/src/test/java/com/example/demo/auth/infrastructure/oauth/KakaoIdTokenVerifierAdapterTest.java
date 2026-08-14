package com.example.demo.auth.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class KakaoIdTokenVerifierAdapterTest {

    @Test
    void 검증된_JWT에서_Kakao_사용자_정보를_추출한다() {
        final JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode("id-token"))
                .thenReturn(Jwt.withTokenValue("id-token")
                        .header("alg", "RS256")
                        .claim("iss", "https://kauth.kakao.com")
                        .claim("email", "user@example.com")
                        .claim("nickname", "Kakao Name")
                        .subject("kakao-subject")
                        .build());

        final var result = new KakaoIdTokenVerifierAdapter(decoder).verify("id-token");

        assertThat(result.subject()).isEqualTo("kakao-subject");
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.name()).isEqualTo("Kakao Name");
    }

    @Test
    void 검증이_실패하면_Kakao_토큰_오류를_던진다() {
        final JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode("invalid")).thenThrow(new IllegalArgumentException("invalid"));

        assertThatThrownBy(() -> new KakaoIdTokenVerifierAdapter(decoder).verify("invalid"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.KAKAO_TOKEN_INVALID));
    }

    @Test
    void subject가_없으면_토큰_오류를_던진다() {
        final JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode("without-subject"))
                .thenReturn(Jwt.withTokenValue("without-subject")
                        .header("alg", "RS256")
                        .claim("iss", "https://kauth.kakao.com")
                        .build());

        assertThatThrownBy(() -> new KakaoIdTokenVerifierAdapter(decoder).verify("without-subject"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.KAKAO_TOKEN_INVALID));
    }
}
