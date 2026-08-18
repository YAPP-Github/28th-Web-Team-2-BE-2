package com.example.demo.report.infrastructure;

import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.application.port.UserReportCommandPort;
import com.example.demo.report.domain.UserReport;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserReportCommandAdapter implements UserReportCommandPort {

    private final UserReportJpaRepository userReportJpaRepository;

    @Override
    public Long save(final CreateUserReportCommand command, final Long storeId) {
        return Objects.requireNonNull(userReportJpaRepository.save(new UserReport(
                storeId, command.itemId(), command.userId(), command.price(), command.unit(), command.amount(),
                command.photoUrl())).id());
    }
}
