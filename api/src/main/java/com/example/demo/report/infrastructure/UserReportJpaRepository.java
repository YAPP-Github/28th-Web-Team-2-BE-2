package com.example.demo.report.infrastructure;

import com.example.demo.report.domain.UserReport;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserReportJpaRepository extends JpaRepository<UserReport, Long> {

    Optional<UserReport> findFirstByItemIdAndRegionIdAndUnitOrderByReportDateDescIdDesc(
            Long itemId, String regionId, String unit);

    @Query("""
            SELECT report
            FROM UserReport report
            WHERE report.storeId IS NOT NULL
              AND report.regionId = :regionId
              AND report.publicPriceDiff < 0
              AND NOT EXISTS (
                  SELECT newer.id
                  FROM UserReport newer
                  WHERE newer.itemId = report.itemId
                    AND newer.storeId = report.storeId
                    AND newer.regionId = report.regionId
                    AND newer.publicPriceDiff < 0
                    AND (
                        newer.reportDate > report.reportDate
                        OR (newer.reportDate = report.reportDate AND newer.id > report.id)
                    )
              )
            """)
    List<UserReport> findLatestCheapReports(@Param("regionId") String regionId);

    Page<UserReport> findAllByUserId(Long userId, Pageable pageable);

    Optional<UserReport> findByIdAndUserId(Long reportId, Long userId);

    List<UserReport> findAllByUserIdAndReportDateBetweenOrderByReportDateAscIdAsc(
            Long userId, LocalDate from, LocalDate to);

    Page<UserReport> findAllByItemIdAndRegionIdAndUnit(
            Long itemId, String regionId, String unit, Pageable pageable);
}
