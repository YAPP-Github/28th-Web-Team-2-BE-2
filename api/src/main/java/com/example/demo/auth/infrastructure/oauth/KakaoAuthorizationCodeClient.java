package com.example.demo.auth.infrastructure.oauth;

import com.example.demo.auth.application.port.KakaoTokenClient;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class KakaoAuthorizationCodeClient implements KakaoTokenClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String GRANT_TYPE = "authorization_code";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    @Autowired
    public KakaoAuthorizationCodeClient(
            @Value("${kakao.oauth.client-id}") final String clientId,
            @Value("${kakao.oauth.client-secret:}") final String clientSecret,
            @Value("${kakao.oauth.test-redirect-uri}") final String redirectUri) {
        this(RestClient.builder().requestFactory(kakaoRequestFactory()), clientId, clientSecret, redirectUri);
    }

    KakaoAuthorizationCodeClient(
            final RestClient.Builder restClientBuilder,
            final String clientId,
            final String clientSecret,
            final String redirectUri) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    private static ClientHttpRequestFactory kakaoRequestFactory() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }

    @Override
    public String exchangeIdToken(final String authorizationCode) {
        try {
            final KakaoTokenResponse response = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form(authorizationCode))
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            return idToken(response);
        } catch (final ApiException exception) {
            throw exception;
        } catch (final RuntimeException exception) {
            throw invalidKakaoToken();
        }
    }

    private MultiValueMap<String, String> form(final String authorizationCode) {
        final LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE);
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        if (StringUtils.hasText(clientSecret)) {
            form.add("client_secret", clientSecret);
        }
        return form;
    }

    private String idToken(final KakaoTokenResponse response) {
        if (response == null || !StringUtils.hasText(response.idToken())) {
            throw invalidKakaoToken();
        }
        return response.idToken();
    }

    private ApiException invalidKakaoToken() {
        return new ApiException(
                ErrorType.KAKAO_TOKEN_INVALID.description(),
                ErrorType.KAKAO_TOKEN_INVALID,
                HttpStatus.UNAUTHORIZED);
    }

    private record KakaoTokenResponse(@JsonProperty("id_token") String idToken) {}
}
