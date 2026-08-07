package com.example.demo.auth.presentation;

import com.example.demo.auth.application.command.KakaoLoginCommand;
import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.usecase.KakaoLoginUseCase;
import com.example.demo.auth.application.usecase.LogoutUseCase;
import com.example.demo.auth.application.usecase.ReissueTokenUseCase;
import com.example.demo.auth.presentation.converter.AuthCommandConverter;
import com.example.demo.auth.presentation.converter.AuthResultConverter;
import com.example.demo.auth.presentation.dto.KakaoLoginRequest;
import com.example.demo.auth.presentation.dto.TokenResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoLoginUseCase kakaoLoginUseCase;
    private final ReissueTokenUseCase reissueTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthCommandConverter commandConverter;
    private final AuthResultConverter resultConverter;
    private final RefreshTokenCookie refreshTokenCookie;

    @PostMapping("/kakao/login")
    @Operation(summary = "Kakao idToken으로 로그인한다")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody final KakaoLoginRequest request) {
        final KakaoLoginCommand command = commandConverter.toKakaoLoginCommand(request);
        final AuthToken token = kakaoLoginUseCase.execute(command);
        return tokenResponse(token);
    }

    @PostMapping("/reissue")
    @Operation(summary = "Refresh Token cookie로 Access Token을 재발급한다")
    public ResponseEntity<TokenResponse> reissue(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) final String refreshToken) {
        final RefreshTokenCommand command = commandConverter.toRefreshTokenCommand(refreshToken);
        final AuthToken token = reissueTokenUseCase.execute(command);
        return tokenResponse(token);
    }

    @PostMapping("/logout")
    @Operation(summary = "Refresh Token을 폐기하고 로그아웃한다")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) final String refreshToken) {
        final LogoutCommand command = commandConverter.toLogoutCommand(refreshToken);
        logoutUseCase.execute(command);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.delete().toString())
                .build();
    }

    private ResponseEntity<TokenResponse> tokenResponse(final AuthToken token) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(token.refreshToken()).toString())
                .body(resultConverter.toTokenResponse(token));
    }
}
