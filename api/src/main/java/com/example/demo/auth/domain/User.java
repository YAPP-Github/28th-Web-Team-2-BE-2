package com.example.demo.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_provider_subject",
                columnNames = {"provider", "provider_subject"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 100)
    private String providerSubject;

    @Column(length = 320)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    private User(
            final UserProvider provider,
            final String providerSubject,
            final String email,
            final String name,
            final UserRole role) {
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public static User kakao(final String providerSubject, final String email, final String name) {
        return new User(UserProvider.KAKAO, providerSubject, email, name, UserRole.USER);
    }

    public Long id() {
        return id;
    }

    public UserRole role() {
        return role;
    }
}
