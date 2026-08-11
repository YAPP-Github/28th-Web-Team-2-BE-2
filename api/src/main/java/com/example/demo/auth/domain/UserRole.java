package com.example.demo.auth.domain;

public enum UserRole {
    USER;

    public String authority() {
        return "ROLE_" + name();
    }
}
