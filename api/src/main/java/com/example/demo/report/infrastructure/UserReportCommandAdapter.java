package com.example.demo.report.infrastructure;

import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.port.UserReportCommandPort;
import com.example.demo.report.domain.UserReport;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserReportCommandAdapter implements UserReportCommandPort {

    private final UserReportJpaRepository userReportJpaRepository;

    @Override
    public UserReport save(
            final CreateUserReportCommand command,
            final Long storeId,
            final Integer publicPriceDiff,
            final BigDecimal priceDiffRate) {
        return Objects.requireNonNull(userReportJpaRepository.saveAndFlush(new UserReport(
                command.regionId(), command.reportType(), storeId, command.itemId(), command.userId(),
                command.price(), command.unit(), command.amount(), publicPriceDiff, priceDiffRate,
                command.photoUrl())));
    }
}
