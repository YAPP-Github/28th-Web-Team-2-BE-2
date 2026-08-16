package com.example.demo.item.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.result.BatchJobStatus;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class BatchJobPersistenceAdapterTest {

    @Autowired
    private BatchJobPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM batch_item_errors");
        jdbcTemplate.update("DELETE FROM batch_job_execution");
    }

    @Test
    void job을_STARTED로_생성하고_최종_상태와_집계를_갱신한다() {
        final Long jobExecutionId = adapter.start("ONLINE_PRICE_COLLECTION");

        adapter.finish(jobExecutionId, BatchJobStatus.PARTIAL, 4, 3, null);

        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM batch_job_execution WHERE job_execution_id = ?", jobExecutionId);
        assertThat(row.get("JOB_NAME")).isEqualTo("ONLINE_PRICE_COLLECTION");
        assertThat(row.get("STATUS")).isEqualTo(BatchJobStatus.PARTIAL.name());
        assertThat(row.get("TOTAL_RECORDS")).isEqualTo(4);
        assertThat(row.get("SUCCESS_RECORDS")).isEqualTo(3);
        assertThat(row.get("STARTED_AT")).isNotNull();
        assertThat(row.get("ENDED_AT")).isNotNull();
    }

    @Test
    void 최종_item_실패는_시도횟수와_정제된_오류만_한건_저장한다() {
        final Long jobExecutionId = adapter.start("ONLINE_PRICE_COLLECTION");

        adapter.recordItemError(
                jobExecutionId,
                1L,
                2,
                1,
                ErrorType.EXTERNAL_API_ERROR.name(),
                ErrorType.EXTERNAL_API_ERROR.description());
        assertThatThrownBy(() -> adapter.recordItemError(
                        jobExecutionId,
                        1L,
                        2,
                        1,
                        ErrorType.EXTERNAL_API_ERROR.name(),
                        ErrorType.EXTERNAL_API_ERROR.description()))
                .isInstanceOf(DataIntegrityViolationException.class);

        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM batch_item_errors WHERE job_execution_id = ?", jobExecutionId);
        assertThat(row.get("ITEM_ID")).isEqualTo(1L);
        assertThat(row.get("CHANNEL_ID")).isEqualTo(2);
        assertThat(row.get("ATTEMPT_COUNT")).isEqualTo(1);
        assertThat(row.get("ERROR_TYPE")).isEqualTo(ErrorType.EXTERNAL_API_ERROR.name());
        assertThat(row.get("ERROR_MESSAGE")).isEqualTo(ErrorType.EXTERNAL_API_ERROR.description());
        assertThat(row).doesNotContainKey("RAW_DATA");
    }
}
