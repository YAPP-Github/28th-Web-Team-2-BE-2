package com.example.demo.auth.application.port;

public interface KakaoTokenClient {

    String exchangeIdToken(String authorizationCode);
}
