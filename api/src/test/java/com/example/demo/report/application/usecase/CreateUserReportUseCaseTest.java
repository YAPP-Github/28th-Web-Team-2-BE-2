package com.example.demo.report.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.command.StoreSnapshot;
import com.example.demo.report.application.port.StoreCommandPort;
import com.example.demo.report.application.port.UserReportCommandPort;
import com.example.demo.report.application.result.CreateUserReportResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
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
        when(itemExistencePort.exists(ITEM_ID)).thenReturn(true);
        final StoreSnapshot storeSnapshot = new StoreSnapshot("장보고 마트", "서울특별시 마포구 월드컵로 1");
        final CreateUserReportCommand command = new CreateUserReportCommand(
                ITEM_ID, USER_ID, PRICE, UNIT, AMOUNT, storeSnapshot, PHOTO_URL);
        when(storeCommandPort.save(storeSnapshot)).thenReturn(STORE_ID);
        when(userReportCommandPort.save(command, STORE_ID)).thenReturn(REPORT_ID);
        final CreateUserReportUseCase useCase = new CreateUserReportUseCase(
                storeCommandPort, userReportCommandPort, itemExistencePort);

        final CreateUserReportResult result = useCase.execute(command);

        assertThat(result.reportId()).isEqualTo(REPORT_ID);
        final InOrder inOrder = inOrder(storeCommandPort, userReportCommandPort);
        inOrder.verify(storeCommandPort).save(storeSnapshot);
        inOrder.verify(userReportCommandPort).save(command, STORE_ID);
    }

    @Test
    void 존재하지_않는_품목은_매장을_저장하기_전에_404로_거부한다() {
        final StoreCommandPort storeCommandPort = mock(StoreCommandPort.class);
        final UserReportCommandPort userReportCommandPort = mock(UserReportCommandPort.class);
        final ItemExistencePort itemExistencePort = mock(ItemExistencePort.class);
        when(itemExistencePort.exists(ITEM_ID)).thenReturn(false);
        final CreateUserReportUseCase useCase = new CreateUserReportUseCase(
                storeCommandPort, userReportCommandPort, itemExistencePort);

        assertThatThrownBy(() -> useCase.execute(new CreateUserReportCommand(
                ITEM_ID, USER_ID, PRICE, UNIT, AMOUNT,
                new StoreSnapshot("장보고 마트", "서울"), PHOTO_URL)))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).httpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(storeCommandPort, userReportCommandPort);
    }
}
