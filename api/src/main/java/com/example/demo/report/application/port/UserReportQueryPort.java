package com.example.demo.report.application.port;

import com.example.demo.report.application.query.MyReportQuery;
import com.example.demo.report.application.query.RegionItemReportQuery;
import com.example.demo.report.domain.UserReport;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface UserReportQueryPort {

    Optional<Integer> findLatestPrice(Long itemId, String regionId, String unit);

    /** 품목 기준 단위와 일치하는 제보만 조회한다. */
    Page<UserReport> findByRegionAndItem(RegionItemReportQuery query, String unit);

    /** 현재 사용자의 제보를 최신순으로 조회한다. */
    Page<UserReport> findByUser(MyReportQuery query);
}
