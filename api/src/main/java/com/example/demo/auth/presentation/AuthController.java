package com.example.demo.auth.presentation;

import com.example.demo.auth.application.command.LoginCommand;
import com.example.demo.auth.application.command.LogoutCommand;
import com.example.demo.auth.application.command.RefreshTokenCommand;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.usecase.LoginUseCase;
import com.example.demo.auth.application.usecase.LogoutUseCase;
import com.example.demo.auth.application.usecase.ReissueTokenUseCase;
import com.example.demo.auth.presentation.converter.AuthCommandConverter;
import com.example.demo.auth.presentation.converter.AuthResultConverter;
import com.example.demo.auth.presentation.dto.LoginRequest;
import com.example.demo.auth.presentation.dto.RefreshTokenRequest;
import com.example.demo.auth.presentation.dto.TokenResponse;
import com.example.demo.auth.presentation.spec.AuthControllerSpec;
import com.example.demo.common.security.AuthPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerSpec {

    private final LoginUseCase loginUseCase;
    private final ReissueTokenUseCase reissueTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthCommandConverter commandConverter;
    private final AuthResultConverter resultConverter;

    @PostMapping("/{providerType}/login")
    @Override
    public ResponseEntity<TokenResponse> login(
            @PathVariable final String providerType,
            @Valid @RequestBody final LoginRequest request) {
        final LoginCommand command = commandConverter.toLoginCommand(providerType, request);
        final AuthToken token = loginUseCase.execute(command);
        return tokenResponse(token);
    }

    @PostMapping("/reissue")
    @Override
    public ResponseEntity<TokenResponse> reissue(
            @Valid @RequestBody final RefreshTokenRequest request) {
        final RefreshTokenCommand command = commandConverter.toRefreshTokenCommand(request);
        final AuthToken token = reissueTokenUseCase.execute(command);
        return tokenResponse(token);
    }

    @PostMapping("/logout")
    @Override
    public ResponseEntity<Void> logout(@AuthenticationPrincipal final AuthPrincipal principal) {
        final LogoutCommand command = commandConverter.toLogoutCommand(principal.userId());
        logoutUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<TokenResponse> tokenResponse(final AuthToken token) {
        return ResponseEntity.ok().body(resultConverter.toTokenResponse(token));
    }
}
