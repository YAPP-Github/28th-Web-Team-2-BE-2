package com.example.demo.report.application.port;

import com.example.demo.report.application.command.CreateUserReportCommand;
import com.example.demo.report.domain.UserReport;
import java.math.BigDecimal;

public interface UserReportCommandPort {

    UserReport save(
            CreateUserReportCommand command,
            Long storeId,
            Integer publicPriceDiff,
            BigDecimal priceDiffRate);
}
