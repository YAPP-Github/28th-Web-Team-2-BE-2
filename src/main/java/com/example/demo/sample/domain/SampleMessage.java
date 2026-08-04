package com.example.demo.sample.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "sample_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class SampleMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String message;

    public SampleMessage(final String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("sample message must not be blank");
        }
        this.message = message;
    }

    public static SampleMessage defaultMessage() {
        return new SampleMessage("Hello from demo");
    }

    public Long id() {
        return id;
    }

    public String message() {
        return message;
    }
}
