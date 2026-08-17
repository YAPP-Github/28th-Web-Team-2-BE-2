package com.example.demo.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "user_id", nullable = false)
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

    @Column(name = "price_diff_rate", precision = 5, scale = 2)
    private BigDecimal priceDiffRate;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    public UserReport(final Long storeId, final Long itemId, final Long userId, final Integer price,
            final String unit, final BigDecimal amount, final String photoUrl) {
        this.storeId = storeId;
        this.itemId = itemId;
        this.userId = userId;
        this.price = price;
        this.unit = unit;
        this.amount = amount;
        this.photoUrl = photoUrl;
        this.reportDate = LocalDate.now();
    }
}
