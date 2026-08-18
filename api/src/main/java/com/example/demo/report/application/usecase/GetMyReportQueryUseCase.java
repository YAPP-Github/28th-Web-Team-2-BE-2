package com.example.demo.report.application.usecase;

import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.application.query.MyReportQuery;
import com.example.demo.report.application.result.MyReportPageResult;
import com.example.demo.report.application.result.MyReportSummaryResult;
import com.example.demo.report.domain.UserReport;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyReportQueryUseCase {

    private final UserReportQueryPort userReportQueryPort;
    private final ItemExistencePort itemExistencePort;

    @Transactional(readOnly = true)
    public MyReportPageResult execute(final MyReportQuery query) {
        final Page<UserReport> reports = userReportQueryPort.findByUser(query);
        final Map<Long, String> itemNames = findItemNames(reports.getContent());
        return new MyReportPageResult(
                toSummaries(reports.getContent(), itemNames),
                query.page(),
                query.size(),
                reports.getTotalElements(),
                reports.getTotalPages(),
                reports.hasNext());
    }

    private Map<Long, String> findItemNames(final List<UserReport> reports) {
        final List<Long> itemIds = reports.stream().map(UserReport::itemId).distinct().toList();
        return itemExistencePort.findNamesByIds(itemIds);
    }

    private List<MyReportSummaryResult> toSummaries(
            final List<UserReport> reports, final Map<Long, String> itemNames) {
        return reports.stream()
                .map(report -> toSummary(report, itemNames.get(report.itemId())))
                .toList();
    }

    private MyReportSummaryResult toSummary(final UserReport report, final String itemName) {
        return new MyReportSummaryResult(
                report.id(),
                itemName,
                report.price(),
                report.unit(),
                report.reportDate(),
                report.regionId(),
                report.publicPriceDiff());
    }
}
