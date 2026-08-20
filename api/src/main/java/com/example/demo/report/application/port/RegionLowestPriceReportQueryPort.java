package com.example.demo.report.application.port;

import com.example.demo.report.application.result.RegionLowestPriceReportsQueryResult;
import java.time.LocalDate;

public interface RegionLowestPriceReportQueryPort {

    RegionLowestPriceReportsQueryResult find(String regionId, LocalDate from, LocalDate to);
}
