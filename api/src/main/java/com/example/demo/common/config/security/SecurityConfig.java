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
                                "/api/v1/news",
                                "/api/v1/regions/nearby",
                                "/api/v1/regions/search",
                                "/api/v1/stores/nearby",
                                "/api/auth/test/kakao/redirect",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/*/login",
                                "/api/auth/reissue")
                        .permitAll()
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
