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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final OAuthIdentityVerifier identityVerifier;
    private final AuthTokenIssuer authTokenIssuer;
    private final UserRepository userRepository;

    public AuthToken execute(final LoginCommand command) {
        if (ProviderType.KAKAO != command.providerType()) {
            throw unsupportedProvider();
        }
        if (!StringUtils.hasText(command.idToken())) {
            throw invalidKakaoToken();
        }
        final OAuthUserInfo userInfo = identityVerifier.verify(command.idToken());
        final User user = findOrCreateUser(command.providerType(), userInfo);
        return authTokenIssuer.issue(user.id(), user.role());
    }

    private User findOrCreateUser(final ProviderType provider, final OAuthUserInfo userInfo) {
        final Optional<User> existingUser = userRepository.findByProviderAndProviderSubject(
                provider, userInfo.subject());
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        return saveOrFindUser(provider, userInfo);
    }

    private User saveOrFindUser(final ProviderType provider, final OAuthUserInfo userInfo) {
        try {
            return userRepository.save(User.oauth(
                    provider,
                    userInfo.subject(),
                    optionalText(userInfo.email()),
                    defaultName(userInfo.name())));
        } catch (final DataIntegrityViolationException exception) {
            return userRepository.findByProviderAndProviderSubject(provider, userInfo.subject())
                    .orElseThrow(() -> exception);
        }
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
