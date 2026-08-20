package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.domain.Item;
import com.example.demo.report.application.command.UpdateUserReportCommand;
import com.example.demo.report.application.port.UserReportCommandPort;
import com.example.demo.report.application.port.UserReportQueryPort;
import com.example.demo.report.domain.UserReport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserReportUseCase {

    private final UserReportQueryPort userReportQueryPort;
    private final UserReportCommandPort userReportCommandPort;
    private final ItemExistencePort itemExistencePort;

    @Transactional
    public void execute(final UpdateUserReportCommand command) {
        final UserReport report = userReportQueryPort
                .findByIdAndUserId(command.reportId(), command.userId())
                .orElseThrow(this::reportNotFound);
        final Item item = itemExistencePort.findById(report.itemId())
                .orElseThrow(this::reportNotFound);
        validateUnit(command.unit(), item.defaultUnit());
        final Integer publicPriceDiff = calculatePublicPriceDiff(command.price(), report);
        report.updateValues(
                command.price(), command.unit(), command.amount(),
                publicPriceDiff, calculatePriceDiffRate(publicPriceDiff, report));
        userReportCommandPort.save(report);
    }

    private Integer calculatePublicPriceDiff(final Integer reportPrice, final UserReport report) {
        if (report.publicPriceDiff() == null) {
            return null;
        }
        final int publicPrice = report.price() - report.publicPriceDiff();
        return reportPrice - publicPrice;
    }

    private void validateUnit(final String requestedUnit, final String defaultUnit) {
        if (defaultUnit == null || !defaultUnit.equals(requestedUnit)) {
            throw ApiException.invalidParameter();
        }
    }

    private BigDecimal calculatePriceDiffRate(
            final Integer publicPriceDiff, final UserReport report) {
        if (publicPriceDiff == null) {
            return null;
        }
        final int publicPrice = report.price() - report.publicPriceDiff();
        if (publicPrice == 0) {
            return null;
        }
        return BigDecimal.valueOf(publicPriceDiff)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(publicPrice), 2, RoundingMode.HALF_UP);
    }

    private ApiException reportNotFound() {
        return new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND);
    }
}
