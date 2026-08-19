package com.example.demo.user.domain;

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
        name = "user_regions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_regions_user_region",
                columnNames = {"user_id", "region_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class UserRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_region_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "region_id", nullable = false, length = 10)
    private String regionId;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    private UserRegion(
            final Long userId,
            final String regionId,
            final boolean isCurrent) {
        this.userId = userId;
        this.regionId = regionId;
        this.isCurrent = isCurrent;
    }

    public static UserRegion interestedIn(final Long userId, final String regionId) {
        return new UserRegion(userId, regionId, false);
    }

    public static UserRegion current(final Long userId, final String regionId) {
        return new UserRegion(userId, regionId, true);
    }

    public void markCurrent() {
        isCurrent = true;
    }
}
