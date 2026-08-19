package com.example.demo.report.infrastructure;

import com.example.demo.report.domain.UserReport;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface UserReportJpaRepository extends JpaRepository<UserReport, Long> {

    @Query("select report from UserReport report where report.publicPriceDiff < 0 "
            + "and report.id in (select max(latest.id) from UserReport latest "
            + "where latest.storeId = report.storeId and latest.itemId = report.itemId "
            + "group by latest.storeId, latest.itemId)")
    List<UserReport> findLatestCheapReports();

    @Query("select report from UserReport report where report.regionId = :regionId "
            + "and report.publicPriceDiff < 0 "
            + "and report.id in (select max(latest.id) from UserReport latest "
            + "where latest.regionId = :regionId and latest.storeId = report.storeId "
            + "and latest.itemId = report.itemId group by latest.storeId, latest.itemId)")
    List<UserReport> findLatestCheapReports(@Param("regionId") String regionId);

    Optional<UserReport> findFirstByItemIdAndRegionIdAndUnitOrderByReportDateDescIdDesc(
            Long itemId, String regionId, String unit);
}
