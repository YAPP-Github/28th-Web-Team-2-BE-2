package com.example.demo.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.common.exception.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class SecurityErrorHandlerTest {

    private final SecurityErrorResponseWriter responseWriter =
            new SecurityErrorResponseWriter(new ObjectMapper());

    @Test
    void 인증되지_않은_요청은_JSON_401을_응답한다() throws Exception {
        final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(responseWriter);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("unauthorized"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
    }

    @Test
    void 잘못된_JWT는_INVALID_TOKEN_401을_응답한다() throws Exception {
        final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(responseWriter);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE, ErrorType.INVALID_TOKEN);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("invalid"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString()).contains("INVALID_TOKEN");
    }

    @Test
    void V1_인증_오류는_공통_V1_응답을_사용한다() throws Exception {
        final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(responseWriter);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/items/1/favorite");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("unauthorized"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString())
                .isEqualTo("{\"code\":\"UNAUTHORIZED\",\"message\":\"로그인이 필요한 서비스입니다.\",\"data\":null}");
    }

    @Test
    void 권한이_없으면_JSON_403을_응답한다() throws Exception {
        final JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(responseWriter);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentAsString()).contains("FORBIDDEN");
    }
}
