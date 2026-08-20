package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.command.AnalyzeReportImageCommand;
import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.command.StoreSnapshot;
import com.example.demo.report.application.command.UpdateUserReportCommand;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.presentation.dto.CreateUserReportRequest;
import com.example.demo.report.presentation.dto.ImageAnalysisRequest;
import com.example.demo.report.presentation.dto.UpdateUserReportRequest;
import org.springframework.stereotype.Component;

@Component
public class UserReportCommandConverter {

    public CreateUserReportCommand toCommand(final Long itemId, final Long userId,
            final CreateUserReportRequest request) {
        final CreateUserReportRequest.StoreRequest store = request.store();
        return new CreateUserReportCommand(itemId, userId, request.regionId(), request.price(), request.unit(),
                request.amount(), ReportType.valueOf(request.reportType()), request.storeId(),
                store == null ? null : new StoreSnapshot(
                        store.id(), store.placeName(), store.placeUrl(), store.categoryName(),
                        store.addressName(), store.roadAddressName(), store.phone(), store.categoryGroupCode(),
                        store.categoryGroupName(), store.x(), store.y(), store.distance()), request.photoUrl());
    }

    public AnalyzeReportImageCommand toAnalyzeReportImageCommand(final ImageAnalysisRequest request) {
        return new AnalyzeReportImageCommand(request.imageUrl(), request.itemId());
    }

    public UpdateUserReportCommand toUpdateCommand(
            final Long reportId, final Long userId, final UpdateUserReportRequest request) {
        return new UpdateUserReportCommand(
                reportId, userId, request.price(), request.unit(), request.amount());
    }
}
