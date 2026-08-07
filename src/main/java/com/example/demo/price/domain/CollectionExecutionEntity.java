package com.example.demo.price.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "collection_executions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionExecutionEntity {

    private static final int MAX_FAILURE_REASON_LENGTH = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false, length = 30)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionStatus status;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    private OffsetDateTime finishedAt;

    @Column(nullable = false)
    private int validOfferCount;

    @Column(length = 1000)
    private String failureReason;

    public CollectionExecutionEntity(
            final Long executionId,
            final Long itemId,
            final String itemName,
            final String channel,
            final CollectionStatus status,
            final OffsetDateTime startedAt,
            final OffsetDateTime finishedAt,
            final int validOfferCount,
            final String failureReason) {
        this.executionId = executionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.channel = channel;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.validOfferCount = validOfferCount;
        this.failureReason = limitFailureReason(failureReason);
    }

    private String limitFailureReason(final String failureReason) {
        if (failureReason == null || failureReason.length() <= MAX_FAILURE_REASON_LENGTH) {
            return failureReason;
        }
        return failureReason.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
