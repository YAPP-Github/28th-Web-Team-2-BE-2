package com.example.demo.report.infrastructure;

import com.example.demo.report.domain.UserReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportJpaRepository extends JpaRepository<UserReport, Long> {

    Optional<UserReport> findFirstByItemIdAndRegionIdAndUnitOrderByReportDateDescIdDesc(
            Long itemId, String regionId, String unit);

    @Query("""
            SELECT report
            FROM UserReport report
            WHERE report.storeId IS NOT NULL
              AND NOT EXISTS (
                  SELECT newer.id
                  FROM UserReport newer
                  WHERE newer.itemId = report.itemId
                    AND newer.storeId = report.storeId
                    AND (
                        newer.reportDate > report.reportDate
                        OR (newer.reportDate = report.reportDate AND newer.id > report.id)
                    )
              )
              AND report.priceDiffRate < 0
              AND report.regionId = :regionId
            """)
    List<UserReport> findLatestCheapReports(@Param("regionId") String regionId);
}
