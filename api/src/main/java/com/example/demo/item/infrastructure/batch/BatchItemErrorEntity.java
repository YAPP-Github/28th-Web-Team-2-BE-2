package com.example.demo.item.infrastructure.batch;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.contract.BatchItemFailure;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "batch_item_errors",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_batch_item_errors_execution_item_channel",
                columnNames = {"job_execution_id", "item_id", "channel_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BatchItemErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id")
    private Long id;

    @Column(name = "job_execution_id", nullable = false)
    private Long jobExecutionId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "channel_id", nullable = false)
    private Integer channelId;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "error_type", nullable = false, length = 100)
    private String errorType;

    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    BatchItemErrorEntity(
            final Long jobExecutionId,
            final BatchItemFailure failure,
            final ErrorType errorType) {
        this.jobExecutionId = jobExecutionId;
        this.itemId = failure.itemId();
        this.channelId = failure.channelId();
        this.attemptCount = failure.attemptCount();
        this.errorType = errorType.name();
        this.errorMessage = errorType.description();
        this.createdAt = Instant.now();
    }
}
