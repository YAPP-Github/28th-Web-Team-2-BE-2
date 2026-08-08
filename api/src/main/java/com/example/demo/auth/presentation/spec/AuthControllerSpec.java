package com.example.demo.auth.presentation.spec;

import com.example.demo.auth.presentation.dto.LoginRequest;
import com.example.demo.auth.presentation.dto.RefreshTokenRequest;
import com.example.demo.auth.presentation.dto.TokenResponse;
import com.example.demo.common.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerSpec {

    @Operation(summary = "OAuth provider의 idToken으로 로그인한다")
    ResponseEntity<TokenResponse> login(String providerType, LoginRequest request);

    @Operation(summary = "Refresh Token으로 Access Token을 재발급한다")
    ResponseEntity<TokenResponse> reissue(RefreshTokenRequest request);

    @Operation(summary = "현재 사용자의 Refresh Token을 폐기하고 로그아웃한다")
    ResponseEntity<Void> logout(AuthPrincipal principal);
}
