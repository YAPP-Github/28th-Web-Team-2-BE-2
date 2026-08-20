package com.example.demo.common.config.security;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.common.security.JwtAccessDeniedHandler;
import com.example.demo.common.security.JwtAuthenticationEntryPoint;
import com.example.demo.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(final TokenProvider tokenProvider) {
        return new JwtAuthenticationFilter(tokenProvider);
    }

    @Bean
    SecurityFilterChain filterChain(
            final HttpSecurity http, final JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/api/kamis/**",
                                "/api/v1/items",
                                "/api/v1/items/*",
                                "/api/v1/news",
                                "/api/v1/regions/nearby",
                                "/api/v1/regions/search",
                                "/api/v1/stores/nearby",
                                "/api/v1/stores/recommendation",
                                "/api/v1/stores/*/reports",
                                "/api/v1/regions/*/reports/lowest-prices",
                                "/api/auth/test/kakao/redirect",
                                "/api/auth/test/token",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/*/login",
                                "/api/auth/reissue")
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/items/*/favorite")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/items/*/favorite")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me/favorite-stores")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/stores/*/favorite")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/stores/*/favorite")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/items/*/reports")
                        .hasRole("USER")
                        // 계약 문서는 업로드를 공개 경로로 적었지만 인증을 요구한다.
                        // 무인증 업로드는 우리 버킷에 임의 파일을 쌓는 경로가 되고,
                        // 제보 작성 자체가 이미 ROLE_USER라 공개로 둘 이유가 없다.
                        .requestMatchers(HttpMethod.POST, "/api/v1/images")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.HEAD, "/api/v1/users/me/regions")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me/regions")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/me/regions")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me/regions/*/current")
                        .hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout")
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            final JwtAuthenticationFilter filter) {
        final FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
