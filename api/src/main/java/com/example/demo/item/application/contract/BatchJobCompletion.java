package com.example.demo.item.application.contract;

import com.example.demo.item.application.result.BatchJobStatus;
import java.util.Objects;

public record BatchJobCompletion(
        BatchJobStatus status,
        int totalRecords,
        int successRecords) {

    private static final String FAILURE_MESSAGE = "온라인 가격 job 실행에 실패했습니다.";

    public BatchJobCompletion {
        Objects.requireNonNull(status, "status must not be null");
        if (totalRecords < 0 || successRecords < 0 || successRecords > totalRecords) {
            throw new IllegalArgumentException("batch job completion counts are invalid");
        }
    }

    public String errorMessage() {
        if (status != BatchJobStatus.FAILED) {
            return null;
        }
        return FAILURE_MESSAGE;
    }
}
