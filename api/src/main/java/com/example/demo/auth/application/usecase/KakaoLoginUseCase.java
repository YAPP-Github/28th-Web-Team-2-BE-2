package com.example.demo.auth.application.usecase;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.command.KakaoLoginCommand;
import com.example.demo.auth.application.port.OAuthIdentityVerifier;
import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.result.OAuthUserInfo;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.UserProvider;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KakaoLoginUseCase {

    private final OAuthIdentityVerifier identityVerifier;
    private final AuthTokenIssuer authTokenIssuer;
    private final UserRepository userRepository;

    @Transactional
    public AuthToken execute(final KakaoLoginCommand command) {
        if (!StringUtils.hasText(command.idToken())) {
            throw invalidKakaoToken();
        }
        final OAuthUserInfo userInfo = identityVerifier.verify(command.idToken());
        final User user = userRepository.findByProviderAndProviderSubject(
                        UserProvider.KAKAO, userInfo.subject())
                .orElseGet(() -> userRepository.save(User.kakao(
                        userInfo.subject(), optionalText(userInfo.email()), defaultName(userInfo.name()))));
        return authTokenIssuer.issue(user.id(), user.role());
    }

    private String optionalText(final String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String defaultName(final String name) {
        return StringUtils.hasText(name) ? name : "Kakao User";
    }

    private ApiException invalidKakaoToken() {
        return new ApiException(
                ErrorType.KAKAO_TOKEN_INVALID.description(),
                ErrorType.KAKAO_TOKEN_INVALID,
                HttpStatus.UNAUTHORIZED);
    }
}
