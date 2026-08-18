package com.example.demo.report.application.usecase;

import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.application.result.DailyReportResult;
import com.example.demo.report.application.result.MyWeeklyReportResult;
import com.example.demo.report.domain.UserReport;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 사용자의 주간 제보 현황을 조회한다.
 *
 * <p>주간은 {@code Asia/Seoul} 기준 오늘이 속한 월요일부터 7일이다. PURCHASE와 OBSERVED를 모두 제보 활동으로 세고, 같은 날짜의 여러
 * 제보는 하루로 집계한다.
 */
@Service
@RequiredArgsConstructor
public class GetMyWeeklyReportQueryUseCase {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DAYS_IN_WEEK = 7;

    private final UserReportQueryPort userReportQueryPort;
    private final ItemExistencePort itemExistencePort;

    @Transactional(readOnly = true)
    public MyWeeklyReportResult execute(final Long userId) {
        final LocalDate weekStart = weekStart();
        final List<UserReport> reports = userReportQueryPort.findByUserInPeriod(
                userId, weekStart, weekStart.plusDays(DAYS_IN_WEEK - 1));
        final Map<LocalDate, UserReport> latestByDate = latestByDate(reports);
        final Map<Long, String> itemNames = findItemNames(latestByDate.values());
        final List<DailyReportResult> dailyReports = IntStream.range(0, DAYS_IN_WEEK)
                .mapToObj(weekStart::plusDays)
                .map(date -> toDailyReport(date, latestByDate.get(date), itemNames))
                .toList();
        return new MyWeeklyReportResult(latestByDate.size(), dailyReports);
    }

    private LocalDate weekStart() {
        return LocalDate.now(SERVICE_ZONE)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 같은 날짜의 제보 중 가장 최근에 저장된 것만 남긴다. 조회 결과는 날짜·id 오름차순이다. */
    private Map<LocalDate, UserReport> latestByDate(final List<UserReport> reports) {
        final Map<LocalDate, UserReport> latestByDate = new LinkedHashMap<>();
        reports.forEach(report -> latestByDate.put(report.reportDate(), report));
        return latestByDate;
    }

    private Map<Long, String> findItemNames(final Collection<UserReport> reports) {
        final List<Long> itemIds =
                reports.stream().map(UserReport::itemId).distinct().toList();
        return itemExistencePort.findNamesByIds(itemIds);
    }

    private DailyReportResult toDailyReport(
            final LocalDate date, final UserReport report, final Map<Long, String> itemNames) {
        if (report == null) {
            return DailyReportResult.empty(date);
        }
        return new DailyReportResult(date, true, report.itemId(), itemNames.get(report.itemId()));
    }
}
