package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.PublicPrice;
import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.port.StoreCommandPort;
import com.example.demo.report.application.port.UserReportCommandPort;
import com.example.demo.report.application.result.CreateUserReportResult;
import com.example.demo.report.domain.UserReport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUserReportUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final StoreCommandPort storeCommandPort;
    private final UserReportCommandPort userReportCommandPort;
    private final ItemExistencePort itemExistencePort;
    private final PublicPriceQueryPort publicPriceQueryPort;

    @Transactional
    public CreateUserReportResult execute(final CreateUserReportCommand command) {
        final Item item = itemExistencePort.findById(command.itemId())
                .orElseThrow(this::itemNotFound);
        validateUnit(command.unit(), item.defaultUnit());
        final PublicPrice publicPrice = findTodayPublicPrice(command);
        final Integer publicPriceDiff = publicPrice == null
                ? null
                : command.price() - publicPrice.price();
        final BigDecimal priceDiffRate = calculatePriceDiffRate(command.price(), publicPrice);
        try {
            final Long storeId = resolveStoreId(command);
            final UserReport report = userReportCommandPort.save(
                    command, storeId, publicPriceDiff, priceDiffRate);
            return new CreateUserReportResult(report.id(), report.itemId(), report.storeId(), report.createdAt());
        } catch (final DataIntegrityViolationException exception) {
            if (!isDuplicateSubmission(exception)) {
                throw exception;
            }
            throw new ApiException(
                    ErrorType.DUPLICATE_USER_REPORT_ERROR.description(),
                    ErrorType.DUPLICATE_USER_REPORT_ERROR,
                    HttpStatus.CONFLICT,
                    exception);
        }
    }

    private Long resolveStoreId(final CreateUserReportCommand command) {
        if (command.storeId() != null && command.store() != null) {
            throw ApiException.invalidParameter();
        }
        if (command.storeId() != null) {
            if (!storeCommandPort.exists(command.storeId())) {
                throw new ApiException(
                        ErrorType.NO_RESOURCE_ERROR.description(),
                        ErrorType.NO_RESOURCE_ERROR,
                        HttpStatus.NOT_FOUND);
            }
            return command.storeId();
        }
        return command.store() == null ? null : storeCommandPort.save(command.store());
    }

    private PublicPrice findTodayPublicPrice(final CreateUserReportCommand command) {
        final LocalDate today = LocalDate.now(SEOUL);
        return publicPriceQueryPort
                .findLatestByItemIdAndRegionId(command.itemId(), command.regionId())
                .filter(publicPrice -> publicPrice.priceDate().equals(today))
                .orElse(null);
    }

    private void validateUnit(final String requestedUnit, final String defaultUnit) {
        if (defaultUnit == null || !defaultUnit.equals(requestedUnit)) {
            throw ApiException.invalidParameter();
        }
    }

    private BigDecimal calculatePriceDiffRate(final Integer reportPrice, final PublicPrice publicPrice) {
        if (publicPrice == null || publicPrice.price() == 0) {
            return null;
        }
        return BigDecimal.valueOf(reportPrice - publicPrice.price())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(publicPrice.price()), 2, RoundingMode.HALF_UP);
    }

    private ApiException itemNotFound() {
        return new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND);
    }

    private boolean isDuplicateSubmission(final Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains("uk_user_reports_submission")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
