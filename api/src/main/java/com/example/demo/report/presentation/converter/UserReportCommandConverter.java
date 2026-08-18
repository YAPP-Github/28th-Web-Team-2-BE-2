package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.command.StoreSnapshot;
import com.example.demo.report.presentation.dto.CreateUserReportRequest;
import org.springframework.stereotype.Component;

@Component
public class UserReportCommandConverter {

    public CreateUserReportCommand toCommand(final Long itemId, final Long userId,
            final CreateUserReportRequest request) {
        final CreateUserReportRequest.StoreRequest store = request.store();
        return new CreateUserReportCommand(itemId, userId, request.price(), request.unit(), request.amount(),
                new StoreSnapshot(store.id(), store.placeName(), store.placeUrl(), store.categoryName(),
                        store.addressName(), store.roadAddressName(), store.phone(), store.categoryGroupCode(),
                        store.categoryGroupName(), store.x(), store.y(), store.distance()), request.photoUrl());
    }
}
