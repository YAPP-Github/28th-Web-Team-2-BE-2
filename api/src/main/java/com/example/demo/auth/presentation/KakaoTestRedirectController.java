package com.example.demo.auth.presentation;

import com.example.demo.auth.application.usecase.KakaoTestRedirectUseCase;
import com.example.demo.auth.presentation.dto.KakaoTestRedirectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/test/kakao")
@ConditionalOnProperty(
        prefix = "kakao.oauth.test-endpoint",
        name = "enabled",
        havingValue = "true")
@RequiredArgsConstructor
public class KakaoTestRedirectController {

    private final KakaoTestRedirectUseCase kakaoTestRedirectUseCase;

    @GetMapping("/redirect")
    public KakaoTestRedirectResponse redirect(
            @RequestParam(value = "code", required = false) final String authorizationCode) {
        return new KakaoTestRedirectResponse(kakaoTestRedirectUseCase.execute(authorizationCode));
    }
}
