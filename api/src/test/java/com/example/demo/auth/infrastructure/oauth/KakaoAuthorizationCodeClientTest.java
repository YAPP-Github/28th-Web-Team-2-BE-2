package com.example.demo.auth.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoAuthorizationCodeClientTest {

    private static final String REDIRECT_URI = "http://localhost:8080/api/auth/test/kakao/redirect";

    @Test
    void Kakao_token_endpoint에_필수_form을_전달하고_idToken만_꺼낸다() {
        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(allOf(
                        containsString("grant_type=authorization_code"),
                        containsString("client_id=client-id"),
                        containsString("code=authorization-code"),
                        containsString("client_secret=client-secret"),
                        containsString("redirect_uri="))))
                .andRespond(withSuccess(
                        "{\"id_token\":\"id-token\",\"access_token\":\"provider-access-token\","
                                + "\"refresh_token\":\"provider-refresh-token\"}",
                        MediaType.APPLICATION_JSON));

        final KakaoAuthorizationCodeClient client = new KakaoAuthorizationCodeClient(
                builder, "client-id", "client-secret", REDIRECT_URI);

        assertThat(client.exchangeIdToken("authorization-code")).isEqualTo("id-token");
        server.verify();
    }

    @Test
    void Kakao_token_endpoint가_실패하면_기존_Kakao_토큰_오류로_변환한다() {
        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\"}"));

        final KakaoAuthorizationCodeClient client = new KakaoAuthorizationCodeClient(
                builder, "client-id", "client-secret", REDIRECT_URI);

        assertThatThrownBy(() -> client.exchangeIdToken("invalid-code"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ErrorType.KAKAO_TOKEN_INVALID));
        server.verify();
    }
}
