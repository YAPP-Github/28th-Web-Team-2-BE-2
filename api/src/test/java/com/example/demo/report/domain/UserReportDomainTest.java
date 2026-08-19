package com.example.demo.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UserReportDomainTest {

    @Test
    void 장소와_가격_제보의_모든_값을_보존한다() {
        final Store store = new Store("166", "장보고", "url", "category", "address", "road", "phone",
                "PM9", "약국", new BigDecimal("127.1"), new BigDecimal("37.5"), 100);
        final UserReport report = new UserReport(
                "1121510100", ReportType.PURCHASE, 1L, 2L, 3L, 3500, "kg", new BigDecimal("1.5"),
                500, new BigDecimal("14.29"), "photo");

        assertThat(store.id()).isNull();
        assertThat(store.kakaoPlaceId()).isEqualTo("166");
        assertThat(store.placeName()).isEqualTo("장보고");
        assertThat(store.placeUrl()).isEqualTo("url");
        assertThat(store.categoryName()).isEqualTo("category");
        assertThat(store.addressName()).isEqualTo("address");
        assertThat(store.roadAddressName()).isEqualTo("road");
        assertThat(store.phone()).isEqualTo("phone");
        assertThat(store.categoryGroupCode()).isEqualTo("PM9");
        assertThat(store.categoryGroupName()).isEqualTo("약국");
        assertThat(store.longitude()).isEqualByComparingTo("127.1");
        assertThat(store.latitude()).isEqualByComparingTo("37.5");
        assertThat(store.distance()).isEqualTo(100);
        assertThat(report.id()).isNull();
        assertThat(report.storeId()).isEqualTo(1L);
        assertThat(report.regionId()).isEqualTo("1121510100");
        assertThat(report.reportType()).isEqualTo(ReportType.PURCHASE);
        assertThat(report.itemId()).isEqualTo(2L);
        assertThat(report.userId()).isEqualTo(3L);
        assertThat(report.price()).isEqualTo(3500);
        assertThat(report.unit()).isEqualTo("kg");
        assertThat(report.amount()).isEqualByComparingTo("1.5");
        assertThat(report.reportDate()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
        assertThat(report.publicPriceDiff()).isEqualTo(500);
        assertThat(report.priceDiffRate()).isEqualByComparingTo("14.29");
        assertThat(report.createdAt()).isNotNull();
        assertThat(report.photoUrl()).isEqualTo("photo");
    }
}
