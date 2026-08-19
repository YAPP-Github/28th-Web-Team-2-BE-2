package com.example.demo.report.domain;

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
@Table(name = "stores", uniqueConstraints = @UniqueConstraint(name = "uk_stores_kakao_place_id", columnNames = "kakao_place_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @Column(name = "kakao_place_id", nullable = false, length = 30)
    private String kakaoPlaceId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "place_url", length = 500)
    private String placeUrl;

    @Column(name = "category_name", length = 255)
    private String categoryName;

    @Column(name = "address_name", nullable = false, length = 255)
    private String addressName;

    @Column(name = "road_address_name", length = 255)
    private String roadAddressName;

    @Column(length = 30)
    private String phone;

    @Column(name = "category_group_code", length = 20)
    private String categoryGroupCode;

    @Column(name = "category_group_name", length = 50)
    private String categoryGroupName;

    @Column(precision = 13, scale = 10)
    private BigDecimal longitude;

    @Column(precision = 13, scale = 10)
    private BigDecimal latitude;

    @Column
    private Integer distance;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "business_hours", length = 2000)
    private String businessHours;

    @Column(name = "open_status", length = 30)
    private String openStatus;

    public Store(final String kakaoPlaceId, final String placeName, final String placeUrl,
            final String categoryName, final String addressName, final String roadAddressName,
            final String phone, final String categoryGroupCode, final String categoryGroupName,
            final BigDecimal longitude, final BigDecimal latitude, final Integer distance) {
        this.kakaoPlaceId = kakaoPlaceId;
        this.placeName = placeName;
        this.placeUrl = placeUrl;
        this.categoryName = categoryName;
        this.addressName = addressName;
        this.roadAddressName = roadAddressName;
        this.phone = phone;
        this.categoryGroupCode = categoryGroupCode;
        this.categoryGroupName = categoryGroupName;
        this.longitude = longitude;
        this.latitude = latitude;
        this.distance = distance;
    }

    public void updateNearbyProviderFields(
            final String placeName,
            final String placeUrl,
            final String addressName,
            final String roadAddressName,
            final String phone,
            final BigDecimal longitude,
            final BigDecimal latitude,
            final Integer distance) {
        this.placeName = placeName;
        this.placeUrl = placeUrl;
        this.addressName = addressName;
        this.roadAddressName = roadAddressName;
        this.phone = phone;
        this.longitude = longitude;
        this.latitude = latitude;
        this.distance = distance;
    }

    public void updateKakaoDetails(
            final String imageUrl,
            final String businessHours,
            final String openStatus) {
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (businessHours != null) {
            this.businessHours = businessHours;
        }
        if (openStatus != null) {
            this.openStatus = openStatus;
        }
    }
}
