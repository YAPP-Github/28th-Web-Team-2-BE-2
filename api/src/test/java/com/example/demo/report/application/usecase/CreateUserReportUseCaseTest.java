package com.example.demo.report.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.command.StoreSnapshot;
import com.example.demo.report.application.port.StoreCommandPort;
import com.example.demo.report.application.port.UserReportCommandPort;
import com.example.demo.report.application.result.CreateUserReportResult;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.UserReport;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;

class CreateUserReportUseCaseTest {

    private static final long ITEM_ID = 10L;
    private static final long USER_ID = 20L;
    private static final long STORE_ID = 30L;
    private static final long REPORT_ID = 40L;
    private static final int PRICE = 4980;
    private static final String UNIT = "1kg";
    private static final BigDecimal AMOUNT = new BigDecimal("2");
    private static final String PHOTO_URL = "https://cdn.example.com/reports/40.jpg";

    @Test
    void 사용자_제보를_매장_스냅샷부터_저장하고_생성된_reportId를_반환한다() {
        final StoreCommandPort storeCommandPort = mock(StoreCommandPort.class);
        final UserReportCommandPort userReportCommandPort = mock(UserReportCommandPort.class);
        final ItemExistencePort itemExistencePort = mock(ItemExistencePort.class);
        final PublicPriceQueryPort publicPriceQueryPort = mock(PublicPriceQueryPort.class);
        when(itemExistencePort.findById(ITEM_ID)).thenReturn(Optional.of(item()));
        when(publicPriceQueryPort.findLatestByItemIdAndRegionId(ITEM_ID, "1121510100"))
                .thenReturn(Optional.empty());
        final StoreSnapshot storeSnapshot = new StoreSnapshot("장보고 마트", "서울특별시 마포구 월드컵로 1");
        final CreateUserReportCommand command = new CreateUserReportCommand(
                ITEM_ID, USER_ID, "1121510100", PRICE, UNIT, AMOUNT, ReportType.PURCHASE, storeSnapshot, PHOTO_URL);
        when(storeCommandPort.save(storeSnapshot)).thenReturn(STORE_ID);
        final UserReport saved = savedReport(STORE_ID);
        when(userReportCommandPort.save(command, STORE_ID, null, null)).thenReturn(saved);
        final CreateUserReportUseCase useCase = new CreateUserReportUseCase(
                storeCommandPort, userReportCommandPort, itemExistencePort, publicPriceQueryPort);

        final CreateUserReportResult result = useCase.execute(command);

        assertThat(result.reportId()).isEqualTo(REPORT_ID);
        final InOrder inOrder = inOrder(storeCommandPort, userReportCommandPort);
        inOrder.verify(storeCommandPort).save(storeSnapshot);
        inOrder.verify(userReportCommandPort).save(command, STORE_ID, null, null);
    }

    @Test
    void 존재하지_않는_품목은_매장을_저장하기_전에_404로_거부한다() {
        final StoreCommandPort storeCommandPort = mock(StoreCommandPort.class);
        final UserReportCommandPort userReportCommandPort = mock(UserReportCommandPort.class);
        final ItemExistencePort itemExistencePort = mock(ItemExistencePort.class);
        final PublicPriceQueryPort publicPriceQueryPort = mock(PublicPriceQueryPort.class);
        when(itemExistencePort.findById(ITEM_ID)).thenReturn(Optional.empty());
        final CreateUserReportUseCase useCase = new CreateUserReportUseCase(
                storeCommandPort, userReportCommandPort, itemExistencePort, publicPriceQueryPort);

        assertThatThrownBy(() -> useCase.execute(new CreateUserReportCommand(
                ITEM_ID, USER_ID, "1121510100", PRICE, UNIT, AMOUNT, ReportType.PURCHASE,
                new StoreSnapshot("장보고 마트", "서울"), PHOTO_URL)))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).httpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(storeCommandPort, userReportCommandPort);
    }

    @Test
    void 매장_없는_제보는_store를_저장하지_않고_null_storeId와_함께_제보를_저장한다() {
        final StoreCommandPort storeCommandPort = mock(StoreCommandPort.class);
        final UserReportCommandPort userReportCommandPort = mock(UserReportCommandPort.class);
        final ItemExistencePort itemExistencePort = mock(ItemExistencePort.class);
        final PublicPriceQueryPort publicPriceQueryPort = mock(PublicPriceQueryPort.class);
        when(itemExistencePort.findById(ITEM_ID)).thenReturn(Optional.of(item()));
        when(publicPriceQueryPort.findLatestByItemIdAndRegionId(ITEM_ID, "1121510100"))
                .thenReturn(Optional.empty());
        final CreateUserReportCommand command = new CreateUserReportCommand(
                ITEM_ID, USER_ID, "1121510100", PRICE, UNIT, AMOUNT, ReportType.OBSERVED, null, PHOTO_URL);
        when(userReportCommandPort.save(command, null, null, null)).thenReturn(savedReport(null));
        final CreateUserReportUseCase useCase = new CreateUserReportUseCase(
                storeCommandPort, userReportCommandPort, itemExistencePort, publicPriceQueryPort);

        assertThat(useCase.execute(command).reportId()).isEqualTo(REPORT_ID);
        verifyNoInteractions(storeCommandPort);
        verify(userReportCommandPort).save(command, null, null, null);
    }

    private Item item() {
        return new Item("감자", UNIT, null, ItemCategory.ROOT_VEGETABLES);
    }

    private UserReport savedReport(final Long storeId) {
        final UserReport report = new UserReport(
                "1121510100", ReportType.PURCHASE, storeId, ITEM_ID, USER_ID, PRICE, UNIT, AMOUNT,
                null, null, PHOTO_URL);
        ReflectionTestUtils.setField(report, "id", REPORT_ID);
        return report;
    }
}
