package com.example.demo.auth.application.port;

public interface KakaoIdentityProvider {

    String verify(String idToken);
}
