package com.example.demo.report.application.port;

import java.util.Optional;

public interface UserReportQueryPort {

    Optional<Integer> findLatestPrice(Long itemId, String regionId, String unit);
}
