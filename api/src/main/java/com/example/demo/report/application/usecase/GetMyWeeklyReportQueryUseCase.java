package com.example.demo.report.application.usecase;

import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.application.result.DailyReportResult;
import com.example.demo.report.application.result.MyWeeklyReportResult;
import com.example.demo.report.domain.UserReport;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 사용자의 주간 제보 현황을 조회한다.
 *
 * <p>주간은 서비스 기준 시각({@code Asia/Seoul})으로 오늘이 속한 월요일부터 7일이다. PURCHASE와 OBSERVED를 모두 제보 활동으로 세고, 같은 날짜의 여러
 * 제보는 하루로 집계한다.
 */
@Service
@RequiredArgsConstructor
public class GetMyWeeklyReportQueryUseCase {

    private final UserReportQueryPort userReportQueryPort;
    private final ItemExistencePort itemExistencePort;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MyWeeklyReportResult execute(final Long userId) {
        final LocalDate weekStart = weekStart();
        final LocalDate weekEnd = weekStart.plusWeeks(1);
        final List<UserReport> reports =
                userReportQueryPort.findByUserInPeriod(userId, weekStart, weekEnd.minusDays(1));
        final Map<LocalDate, UserReport> firstByDate = firstByDate(reports);
        final Map<Long, String> itemNames = findItemNames(firstByDate.values());
        final List<DailyReportResult> dailyReports = weekStart.datesUntil(weekEnd)
                .map(date -> toDailyReport(date, firstByDate.get(date), itemNames))
                .toList();
        return new MyWeeklyReportResult(firstByDate.size(), dailyReports);
    }

    private LocalDate weekStart() {
        return LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 같은 날짜의 제보 중 가장 먼저 등록된 것을 그날의 대표로 삼는다. */
    private Map<LocalDate, UserReport> firstByDate(final List<UserReport> reports) {
        return reports.stream().collect(Collectors.toMap(
                UserReport::reportDate,
                Function.identity(),
                BinaryOperator.minBy(Comparator.comparing(UserReport::id))));
    }

    private Map<Long, String> findItemNames(final Collection<UserReport> reports) {
        return itemExistencePort.findNamesByIds(
                reports.stream().map(UserReport::itemId).toList());
    }

    private DailyReportResult toDailyReport(
            final LocalDate date, final UserReport report, final Map<Long, String> itemNames) {
        if (report == null) {
            return new DailyReportResult(date, false, null, null);
        }
        return new DailyReportResult(date, true, report.itemId(), itemNames.get(report.itemId()));
    }
}
