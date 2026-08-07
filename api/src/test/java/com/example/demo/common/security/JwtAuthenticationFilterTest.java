package com.example.demo.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void Bearer_Access_Token을_인증_주체로_변환한다() throws Exception {
        final TokenProvider tokenProvider = mock(TokenProvider.class);
        when(tokenProvider.parseAccessTokenPayload("access-token"))
                .thenReturn(new TokenPayload("kakao-subject", Instant.now().plusSeconds(60)));
        final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        final AuthPrincipal principal = (AuthPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        assertThat(principal.subject()).isEqualTo("kakao-subject");
    }

    @Test
    void 잘못된_Access_Token은_context를_비우고_오류를_기록한다() throws Exception {
        final TokenProvider tokenProvider = mock(TokenProvider.class);
        when(tokenProvider.parseAccessTokenPayload("invalid"))
                .thenThrow(new ApiException("invalid", ErrorType.INVALID_TOKEN, org.springframework.http.HttpStatus.UNAUTHORIZED));
        final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE))
                .isEqualTo(ErrorType.INVALID_TOKEN);
    }
}
