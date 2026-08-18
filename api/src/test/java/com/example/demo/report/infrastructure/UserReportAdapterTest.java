package com.example.demo.report.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.command.StoreSnapshot;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.UserReport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserReportAdapterTest {

    @Test
    void 제보를_저장하고_도메인_결과를_반환한다() {
        final UserReportJpaRepository reports = mock(UserReportJpaRepository.class);
        final UserReport saved = new UserReport(
                "1121510100", ReportType.OBSERVED, 1L, 2L, 3L, 3500, "kg", BigDecimal.ONE, null);
        ReflectionTestUtils.setField(saved, "id", 99L);
        when(reports.saveAndFlush(org.mockito.ArgumentMatchers.any(UserReport.class))).thenReturn(saved);

        assertThat(new UserReportCommandAdapter(reports).save(
                new CreateUserReportCommand(2L, 3L, "1121510100", 3500, "kg", BigDecimal.ONE,
                        ReportType.OBSERVED, new StoreSnapshot("store", "address"), null),
                1L, 0, BigDecimal.ZERO)).isSameAs(saved);
    }
}
