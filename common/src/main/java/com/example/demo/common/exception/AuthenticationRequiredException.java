package com.example.demo.common.exception;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException() {
        super("authentication is required");
    }
}
