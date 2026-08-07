package com.example.demo.common.security;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.TokenPayload;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".tokenError";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    public JwtAuthenticationFilter(final TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain)
            throws ServletException, IOException {
        try {
            final String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                final TokenPayload payload = tokenProvider.parseAccessTokenPayload(token);
                final var authentication = new UsernamePasswordAuthenticationToken(
                        new AuthPrincipal(payload.subject()), null, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (final ApiException exception) {
            SecurityContextHolder.clearContext();
            request.setAttribute(TOKEN_ERROR_ATTRIBUTE, exception.errorType());
        } catch (final RuntimeException exception) {
            SecurityContextHolder.clearContext();
            request.setAttribute(TOKEN_ERROR_ATTRIBUTE, ErrorType.INVALID_TOKEN);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(final HttpServletRequest request) {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
