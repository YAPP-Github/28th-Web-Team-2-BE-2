package com.example.demo.common.security;

import com.example.demo.common.exception.ApiErrorResponse;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.presentation.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpStatus status,
            final ErrorType errorType)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        final Object body = request.getRequestURI().startsWith("/api/v1/")
                ? new ApiResponse<>(errorType.name(), errorType.description(), null)
                : new ApiErrorResponse(errorType.name(), errorType.description());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
