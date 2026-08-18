package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemExistencePort;
import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.port.StoreCommandPort;
import com.example.demo.report.application.port.UserReportCommandPort;
import com.example.demo.report.application.result.CreateUserReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUserReportUseCase {

    private final StoreCommandPort storeCommandPort;
    private final UserReportCommandPort userReportCommandPort;
    private final ItemExistencePort itemExistencePort;

    @Transactional
    public CreateUserReportResult execute(final CreateUserReportCommand command) {
        if (!itemExistencePort.exists(command.itemId())) {
            throw new ApiException(ErrorType.NO_RESOURCE_ERROR.description(), ErrorType.NO_RESOURCE_ERROR,
                    HttpStatus.NOT_FOUND);
        }
        final Long storeId = storeCommandPort.save(command.store());
        return new CreateUserReportResult(userReportCommandPort.save(command, storeId));
    }
}
