package com.example.demo.report.infrastructure;

import com.example.demo.report.domain.UserReport;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportJpaRepository extends JpaRepository<UserReport, Long> {

    @Query("select report from UserReport report where report.publicPriceDiff < 0 "
            + "and report.id in (select max(latest.id) from UserReport latest "
            + "where latest.storeId = report.storeId and latest.itemId = report.itemId "
            + "and latest.publicPriceDiff < 0 group by latest.storeId, latest.itemId)")
    List<UserReport> findLatestCheapReports();

    Optional<UserReport> findFirstByItemIdAndRegionIdAndUnitOrderByReportDateDescIdDesc(
            Long itemId, String regionId, String unit);
}
