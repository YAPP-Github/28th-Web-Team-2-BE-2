package com.example.demo.common.security;

import com.example.demo.common.exception.ErrorType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {

    public void write(
            final HttpServletResponse response, final HttpStatus status, final ErrorType errorType)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"errorType\":\""
                + errorType.name()
                + "\",\"errorMessage\":\""
                + errorType.description()
                + "\"}");
    }
}
