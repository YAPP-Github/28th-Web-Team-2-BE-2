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

    /** 저장 수량 소수점 자리. DB가 `NUMERIC(10,3)`이라 그 이상은 어차피 잘린다. */
    private static final int AMOUNT_SCALE = 3;

    private static final BigDecimal GRAMS_PER_KILOGRAM = BigDecimal.valueOf(1000);

    /** {@code user_reports.price}가 {@code Integer}라 환산 결과도 여기까지만 담긴다. */
    private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(Integer.MAX_VALUE);

    private final StoreCommandPort storeCommandPort;
    private final UserReportCommandPort userReportCommandPort;
    private final ItemExistencePort itemExistencePort;
    private final PublicPriceQueryPort publicPriceQueryPort;

    @Transactional
    public CreateUserReportResult execute(final CreateUserReportCommand command) {
        final Item item = itemExistencePort.findById(command.itemId())
                .orElseThrow(this::itemNotFound);
        final CreateUserReportCommand normalized = normalizeQuantity(command, item.defaultUnit());
        final PublicPrice publicPrice = findTodayPublicPrice(command);
        // 공공가격은 기준 단위 1개분 가격이다. 환산 전 값을 쓰면 "2kg에 8000원"이 1kg 공공가격과
        // 그대로 비교돼 차이가 수량 배수만큼 어긋난다.
        final Integer publicPriceDiff = publicPrice == null
                ? null
                : normalized.price() - publicPrice.price();
        final BigDecimal priceDiffRate = calculatePriceDiffRate(normalized.price(), publicPrice);
        try {
            final Long storeId = resolveStoreId(command);
            final UserReport report = userReportCommandPort.save(
                    normalized, storeId, publicPriceDiff, priceDiffRate);
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
     * 요청한 수량·단위를 품목 기준 단위({@code items.default_unit})로 환산한 사본을 돌려준다.
     *
     * <p>기준 단위는 수량 접두사가 붙은 표기다({@code 1kg} · {@code 100g} · {@code 1개} ·
     * {@code 1포기}). 반면 클라이언트는 사용자에게 보여 준 표기를 그대로 보내서 접두사가 없다
     * ({@code kg} · {@code g} · {@code 개} · {@code 포기}). 같은 단위인데 글자가 달라
     * 그동안 모든 제보가 400으로 떨어졌다.
     *
     * <p>수량과 가격을 함께 옮긴다 — {@code 1kg} 품목에 "2kg에 8000원"이 오면
     * {@code price=4000, amount=1, unit=1kg}으로 저장한다. 저장값이 항상 "기준 단위 1개분의
     * 가격"이어야 제보끼리, 그리고 공공가격과 비교할 수 있다({@code publicPriceDiff}가 수량을
     * 보지 않고 {@code price}를 그대로 빼기 때문에 더욱 그렇다).
     *
     * <p>{@code price}가 {@code Integer}라 나눗셈에서 원 단위 반올림이 생긴다("3개에 8000원"
     * → 개당 2667원). 비교가 목적인 값이라 원 미만은 버려도 되지만, 사용자가 입력한 원본
     * 수량·가격은 이 시점에 사라진다.
     *
     * <p>낱개 단위({@code 개} · {@code 포기})는 무게로 환산할 수 없어 같은 단위끼리만 받는다.
     */
    private CreateUserReportCommand normalizeQuantity(
            final CreateUserReportCommand command, final String defaultUnit) {
        if (defaultUnit == null || command.unit() == null || command.amount() == null) {
            throw ApiException.invalidParameter();
        }
        // 이미 기준 단위 1개분이면 손대지 않는다 — 환산할 게 없는데 자릿수만 바꿔 쓰지 않는다.
        if (defaultUnit.equals(command.unit().trim())
                && command.amount().compareTo(BigDecimal.ONE) == 0) {
            return command;
        }
        final Measure requested = parseMeasure(command.unit());
        final Measure standard = parseMeasure(defaultUnit);
        final BigDecimal requestedScale = scaleOf(requested.symbol());
        final BigDecimal standardScale = scaleOf(standard.symbol());

        // 같은 차원(무게끼리, 또는 같은 낱개 단위끼리)이 아니면 환산할 방법이 없다.
        if (requestedScale == null || standardScale == null) {
            if (!requested.symbol().equalsIgnoreCase(standard.symbol())) {
                throw ApiException.invalidParameter();
            }
            return toStandard(command, defaultUnit, command.amount()
                    .multiply(requested.quantity())
                    .divide(standard.quantity(), AMOUNT_SCALE, RoundingMode.HALF_UP));
        }

        // 요청 수량을 기준 단위 몇 개분인지로 환산한다. 예) 500g → 1kg 기준 0.5
        final BigDecimal requestedTotal = command.amount()
                .multiply(requested.quantity())
                .multiply(requestedScale);
        final BigDecimal standardTotal = standard.quantity().multiply(standardScale);
        return toStandard(command, defaultUnit,
                requestedTotal.divide(standardTotal, AMOUNT_SCALE, RoundingMode.HALF_UP));
    }

    /** 기준 단위 {@code amount}개분에 {@code command.price()}원 → 1개분 가격으로 옮긴다. */
    private CreateUserReportCommand toStandard(
            final CreateUserReportCommand command, final String defaultUnit, final BigDecimal amount) {
        // 기준 단위의 1/1000보다 작은 수량은 소수점 3자리(DB `NUMERIC(10,3)`)에 담기지 않는다.
        // 0으로 저장하면 "0kg에 3000원"이 되고 아래 나눗셈도 성립하지 않는다.
        if (amount.signum() <= 0) {
            throw ApiException.invalidParameter();
        }
        final BigDecimal unitPrice = BigDecimal.valueOf(command.price())
                .divide(amount, 0, RoundingMode.HALF_UP);
        // 아주 작은 수량(1g → 1kg 기준 0.001)은 가격을 1000배로 키운다. Integer를 넘으면 받지 않는다.
        if (unitPrice.compareTo(MAX_PRICE) > 0 || unitPrice.signum() <= 0) {
            throw ApiException.invalidParameter();
        }
        return command.withQuantity(defaultUnit, BigDecimal.ONE, unitPrice.intValueExact());
    }

    /** {@code 1kg} → (1, kg) · {@code 100g} → (100, g) · {@code kg} → (1, kg). */
    private Measure parseMeasure(final String unit) {
        final String trimmed = unit.trim();
        final var matcher = UNIT_QUANTITY_PREFIX.matcher(trimmed);
        final BigDecimal quantity = matcher.lookingAt()
                ? new BigDecimal(matcher.group())
                : BigDecimal.ONE;
        final String symbol = trimmed.substring(matcher.lookingAt() ? matcher.end() : 0).trim();
        if (symbol.isEmpty() || quantity.signum() <= 0) {
            throw ApiException.invalidParameter();
        }
        return new Measure(quantity, symbol);
    }

    /** 무게 단위를 g 기준 배율로. 무게가 아니면 {@code null}(= 환산 불가). */
    private BigDecimal scaleOf(final String symbol) {
        if ("kg".equalsIgnoreCase(symbol)) {
            return GRAMS_PER_KILOGRAM;
        }
        if ("g".equalsIgnoreCase(symbol)) {
            return BigDecimal.ONE;
        }
        return null;
    }

    /** 단위 표기를 (수량, 기호)로 쪼갠 값. {@code 100g} = 100 × g. */
    private record Measure(BigDecimal quantity, String symbol) {}

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
