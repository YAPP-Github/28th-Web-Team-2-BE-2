package com.example.demo.common.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    @Test
    void 프론트_배포_origin을_CORS로_허용한다() throws Exception {
        final CorsConfigurationSource source = new CorsConfig().corsConfigurationSource();

        final CorsConfiguration configuration = source.getCorsConfiguration(request("http://localhost:5173"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOriginPatterns())
                .containsExactlyInAnyOrder(
                        "http://localhost:3000",
                        "http://localhost:[*]",
                        "http://192.168.0.100:3000",
                        "http://192.168.0.100:[*]",
                        "https://marketgo.kro.kr",
                        "https://marketgo.kro.kr:[*]",
                        "http://180.233.242.210",
                        "http://180.233.242.210:[*]");
        assertThat(configuration.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(configuration.checkOrigin("http://192.168.0.100:8080")).isEqualTo("http://192.168.0.100:8080");
        assertThat(configuration.checkOrigin("https://marketgo.kro.kr:8443")).isEqualTo("https://marketgo.kro.kr:8443");
        assertThat(configuration.checkOrigin("http://180.233.242.210:4200")).isEqualTo("http://180.233.242.210:4200");
        assertThat(configuration.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).containsExactly("*");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    private HttpServletRequest request(final String origin) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/items");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        return request;
    }
}
