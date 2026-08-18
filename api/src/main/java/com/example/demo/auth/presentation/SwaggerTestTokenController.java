package com.example.demo.auth.presentation;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.auth.presentation.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/test")
@Profile("swagger-test")
@ConditionalOnProperty(
        prefix = "jwt.test-token-endpoint",
        name = "enabled",
        havingValue = "true")
@RequiredArgsConstructor
public class SwaggerTestTokenController {

    private final TokenProvider tokenProvider;

    @GetMapping("/token")
    public TokenResponse createToken(@RequestParam final Long userId) {
        return new TokenResponse(tokenProvider.createAccessToken(userId, UserRole.USER));
    }
}
