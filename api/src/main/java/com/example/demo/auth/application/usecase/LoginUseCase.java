package com.example.demo.auth.application.usecase;

import com.example.demo.auth.application.AuthTokenIssuer;
import com.example.demo.auth.application.command.LoginCommand;
import com.example.demo.auth.application.port.OAuthIdentityVerifier;
import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.auth.application.result.AuthToken;
import com.example.demo.auth.application.result.OAuthUserInfo;
import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final OAuthIdentityVerifier identityVerifier;
    private final AuthTokenIssuer authTokenIssuer;
    private final UserRepository userRepository;

    @Transactional
    public AuthToken execute(final LoginCommand command) {
        if (ProviderType.KAKAO != command.providerType()) {
            throw unsupportedProvider();
        }
        if (!StringUtils.hasText(command.idToken())) {
            throw invalidKakaoToken();
        }
        final OAuthUserInfo userInfo = identityVerifier.verify(command.idToken());
        final User user = userRepository.findByProviderAndProviderSubject(
                        command.providerType(), userInfo.subject())
                .orElseGet(() -> userRepository.save(User.oauth(
                        command.providerType(),
                        userInfo.subject(),
                        optionalText(userInfo.email()),
                        defaultName(userInfo.name()))));
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

    private ApiException unsupportedProvider() {
        return new ApiException(
                ErrorType.INVALID_PARAMETER_ERROR.description(),
                ErrorType.INVALID_PARAMETER_ERROR,
                HttpStatus.BAD_REQUEST);
    }
}
