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
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUserReportUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /** 기준 단위 앞에 붙는 수량({@code 1kg}의 {@code 1}, {@code 100g}의 {@code 100}). */
    private static final Pattern UNIT_QUANTITY_PREFIX = Pattern.compile("^\\d+(?:\\.\\d+)?");

    private final StoreCommandPort storeCommandPort;
    private final UserReportCommandPort userReportCommandPort;
    private final ItemExistencePort itemExistencePort;
    private final PublicPriceQueryPort publicPriceQueryPort;

    @Transactional
    public CreateUserReportResult execute(final CreateUserReportCommand command) {
        final Item item = itemExistencePort.findById(command.itemId())
                .orElseThrow(this::itemNotFound);
        final String unit = resolveUnit(command.unit(), item.defaultUnit());
        final PublicPrice publicPrice = findTodayPublicPrice(command);
        final Integer publicPriceDiff = publicPrice == null
                ? null
                : command.price() - publicPrice.price();
        final BigDecimal priceDiffRate = calculatePriceDiffRate(command.price(), publicPrice);
        try {
            final Long storeId = resolveStoreId(command);
            final UserReport report = userReportCommandPort.save(
                    command.withUnit(unit), storeId, publicPriceDiff, priceDiffRate);
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

    /**
     * 요청 단위를 품목 기준 단위({@code items.default_unit})로 환산해 돌려준다.
     *
     * <p>기준 단위는 수량 접두사가 붙은 표기다({@code 1kg} · {@code 100g} · {@code 1개} ·
     * {@code 1포기}). 반면 클라이언트는 사용자에게 보여 준 표기를 그대로 보내서 접두사가 없다
     * ({@code kg} · {@code g} · {@code 개} · {@code 포기}). 같은 단위인데 글자가 달라
     * 그동안 모든 제보가 400으로 떨어졌다 — 여기서 두 표기를 같은 것으로 받아들인다.
     *
     * <p>돌려주는 값은 항상 기준 단위 원본이다. 저장 표기를 한 가지로 모아야 기존 제보와
     * 비교할 수 있기 때문이다. 기준 단위와 다른 단위(1kg 품목에 {@code g})는 그대로 400이다 —
     * 수량 환산은 {@code price}가 어느 수량의 가격인지 정하지 않은 채로는 할 수 없다
     * ({@code amount}는 지금 어떤 계산에도 쓰이지 않는다).
     */
    private String resolveUnit(final String requestedUnit, final String defaultUnit) {
        if (defaultUnit == null || requestedUnit == null) {
            throw ApiException.invalidParameter();
        }
        final String requested = requestedUnit.trim();
        if (defaultUnit.equalsIgnoreCase(requested)
                || stripQuantityPrefix(defaultUnit).equalsIgnoreCase(requested)) {
            return defaultUnit;
        }
        throw ApiException.invalidParameter();
    }

    /** {@code 1kg} → {@code kg}, {@code 100g} → {@code g}. 접두 수량이 없으면 그대로 둔다. */
    private String stripQuantityPrefix(final String unit) {
        return UNIT_QUANTITY_PREFIX.matcher(unit).replaceFirst("").trim();
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
