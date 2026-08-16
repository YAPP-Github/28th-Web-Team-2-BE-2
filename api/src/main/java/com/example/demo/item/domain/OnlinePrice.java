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
        name = "online_prices",
        indexes = @Index(
                name = "idx_online_prices_item_channel_date",
                columnList = "item_id, channel_id, created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class OnlinePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "online_price_id")
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "channel_id", nullable = false)
    private Integer channelId;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer unit;

    @Column(name = "product_url", length = 500)
    private String productUrl;

    @Column(name = "delivery_note", length = 50)
    private String deliveryNote;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    public OnlinePrice(
            final Long itemId,
            final Integer channelId,
            final String itemName,
            final String productName,
            final Integer price,
            final Integer unit,
            final String productUrl,
            final String deliveryNote,
            final LocalDate createdAt) {
        validate(itemId, channelId, productName, price, unit, createdAt);
        this.itemId = itemId;
        this.channelId = channelId;
        this.itemName = itemName;
        this.productName = productName;
        this.price = price;
        this.unit = unit;
        this.productUrl = productUrl;
        this.deliveryNote = deliveryNote;
        this.createdAt = createdAt;
    }

    private void validate(
            final Long itemId,
            final Integer channelId,
            final String productName,
            final Integer price,
            final Integer unit,
            final LocalDate createdAt) {
        if (itemId == null || itemId <= 0
                || channelId == null || channelId <= 0
                || productName == null || productName.isBlank()
                || price == null || price < 0
                || unit == null || unit <= 0
                || createdAt == null) {
            throw new IllegalArgumentException("online price fields are invalid");
        }
    }
}
