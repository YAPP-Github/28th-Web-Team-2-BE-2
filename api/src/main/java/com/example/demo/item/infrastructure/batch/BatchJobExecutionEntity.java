package com.example.demo.item.infrastructure.batch;

import com.example.demo.item.application.result.BatchJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "batch_job_execution")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BatchJobExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_execution_id")
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchJobStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords;

    @Column(name = "success_records", nullable = false)
    private Integer successRecords;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    BatchJobExecutionEntity(final String jobName) {
        this.jobName = jobName;
        this.status = BatchJobStatus.STARTED;
        this.startedAt = Instant.now();
        this.totalRecords = 0;
        this.successRecords = 0;
    }

    Long id() {
        return id;
    }

    void finish(
            final BatchJobStatus status,
            final int totalRecords,
            final int successRecords,
            final String errorMessage) {
        this.status = status;
        this.endedAt = Instant.now();
        this.totalRecords = totalRecords;
        this.successRecords = successRecords;
        this.errorMessage = errorMessage;
    }
}
