package com.example.demo.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void Bearer_Access_Token을_내부_user_id와_role로_변환한다() throws Exception {
        final TokenProvider tokenProvider = mock(TokenProvider.class);
        when(tokenProvider.parseAccessTokenPayload("access-token"))
                .thenReturn(new AccessTokenPayload(1L, UserRole.USER));
        final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
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
