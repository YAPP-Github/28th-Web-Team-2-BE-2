package com.example.demo.price.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "online_prices", uniqueConstraints = @UniqueConstraint(
        name = "uk_online_price_daily_product",
        columnNames = {"item_id", "channel", "product_name", "created_at"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnlinePriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChannelCode channel;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_url", length = 500)
    private String productUrl;

    @Column(nullable = false)
    private int price;

    @Column(name = "price_per_100g")
    private Integer pricePer100g;

    @Column(nullable = false)
    private int unit;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    public OnlinePriceEntity(
            final Long itemId,
            final ChannelCode channel,
            final String itemName,
            final String productName,
            final String productUrl,
            final int price,
            final Integer pricePer100g,
            final int unit,
            final LocalDate createdAt) {
        this.itemId = itemId;
        this.channel = channel;
        this.itemName = itemName;
        this.productName = productName;
        this.productUrl = productUrl;
        this.price = price;
        this.pricePer100g = pricePer100g;
        this.unit = unit;
        this.createdAt = createdAt;
    }

    public void update(final String itemName, final String productName, final String productUrl,
            final int price, final Integer pricePer100g, final int unit) {
        this.itemName = itemName;
        this.productName = productName;
        this.productUrl = productUrl;
        this.price = price;
        this.pricePer100g = pricePer100g;
        this.unit = unit;
    }
}
