package com.example.demo.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "user_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class UserReport {

    /** 제보 기준일은 서비스 기준 시간대로 고정한다. JVM 기본 시간대(배포 컨테이너는 UTC)를 따르면
     * KST 00:00~09:00 제보가 전날로 기록되어 주간·일자 집계에서 빠진다. */
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "region_id", length = 10)
    private String regionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 20)
    private ReportType reportType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal amount;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "public_price_diff")
    private Integer publicPriceDiff;

    @Column(name = "price_diff_rate", precision = 14, scale = 2)
    private BigDecimal priceDiffRate;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UserReport(final String regionId, final ReportType reportType, final Long storeId,
            final Long itemId, final Long userId, final Integer price, final String unit,
            final BigDecimal amount, final String photoUrl) {
        this(regionId, reportType, storeId, itemId, userId, price, unit, amount, null, null, photoUrl);
    }

    public UserReport(final String regionId, final ReportType reportType, final Long storeId,
            final Long itemId, final Long userId, final Integer price, final String unit,
            final BigDecimal amount, final Integer publicPriceDiff, final BigDecimal priceDiffRate,
            final String photoUrl) {
        this.storeId = storeId;
        this.regionId = regionId;
        this.reportType = reportType;
        this.itemId = itemId;
        this.userId = userId;
        this.price = price;
        this.unit = unit;
        this.amount = amount;
        this.publicPriceDiff = publicPriceDiff;
        this.priceDiffRate = priceDiffRate;
        this.photoUrl = photoUrl;
        this.reportDate = LocalDate.now(SERVICE_ZONE);
        this.createdAt = Instant.now();
    }
}
