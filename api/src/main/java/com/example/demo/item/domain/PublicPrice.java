package com.example.demo.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(
        name = "public_prices",
        indexes = @Index(name = "idx_public_prices_item_region_date", columnList = "item_id, region_id, price_date"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PublicPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "public_price_id")
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "region_id", nullable = false, length = 20)
    private String regionId;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    public PublicPrice(
            final Long itemId,
            final String regionId,
            final Integer price,
            final LocalDate priceDate) {
        if (itemId == null || regionId == null || regionId.isBlank() || price == null || priceDate == null) {
            throw new IllegalArgumentException("public price fields must not be blank");
        }
        if (price < 0) {
            throw new IllegalArgumentException("public price must not be negative");
        }
        this.itemId = itemId;
        this.regionId = regionId;
        this.price = price;
        this.priceDate = priceDate;
    }
}
