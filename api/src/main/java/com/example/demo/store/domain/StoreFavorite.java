package com.example.demo.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(
        name = "store_favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_store_favorites_user_store",
                columnNames = {"user_id", "store_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class StoreFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_favorite_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public StoreFavorite(final Long userId, final Long storeId) {
        this.userId = userId;
        this.storeId = storeId;
        this.createdAt = Instant.now();
    }
}
