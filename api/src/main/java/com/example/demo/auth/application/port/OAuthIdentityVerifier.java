package com.example.demo.auth.application.port;

import com.example.demo.auth.application.result.OAuthUserInfo;

public interface OAuthIdentityVerifier {

    OAuthUserInfo verify(String idToken);
}
