package com.example.demo.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(
        name = "stores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stores_kakao_place_id",
                columnNames = "kakao_place_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @Column(name = "kakao_place_id", nullable = false, length = 100)
    private String kakaoPlaceId;

    @Column(name = "store_name", nullable = false, length = 200)
    private String storeName;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "address_name", length = 500)
    private String addressName;

    @Column(name = "road_address_name", length = 500)
    private String roadAddressName;

    @Column(length = 100)
    private String phone;

    @Column(name = "place_url", length = 2048)
    private String placeUrl;

    public Store(
            final String kakaoPlaceId,
            final String storeName,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final String addressName,
            final String roadAddressName,
            final String phone,
            final String placeUrl) {
        this.kakaoPlaceId = kakaoPlaceId;
        updateProviderFields(
                storeName,
                latitude,
                longitude,
                addressName,
                roadAddressName,
                phone,
                placeUrl);
    }

    public void updateProviderFields(
            final String storeName,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final String addressName,
            final String roadAddressName,
            final String phone,
            final String placeUrl) {
        this.storeName = storeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.addressName = addressName;
        this.roadAddressName = roadAddressName;
        this.phone = phone;
        this.placeUrl = placeUrl;
    }
}
