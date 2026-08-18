package com.example.demo.report.application.port;

import com.example.demo.report.application.command.CreateUserReportCommand;

public interface UserReportCommandPort {
    Long save(CreateUserReportCommand command, Long storeId);
}
