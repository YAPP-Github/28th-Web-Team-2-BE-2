package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class KakaoStoreDetailParserTest {

    @Test
    void org_이미지와_영업시간_영업상태를_파싱한다() {
        final var result = KakaoStoreDetailParser.parse(Jsoup.parse(
                "<meta property='og:image' content='https://place.map.kakao.com/org.jpg'>"
                        + "<div class='list_operation'><li>월 09:00 - 18:00</li></div>"
                        + "<span class='open_state'>영업중</span>"));

        assertThat(result.imageUrl()).isEqualTo("https://place.map.kakao.com/org.jpg");
        assertThat(result.businessHours()).containsExactly("월 09:00 - 18:00");
        assertThat(result.openStatus()).isEqualTo("OPEN");
    }

    @Test
    void 정보가_없으면_null을_반환한다() {
        final var result = KakaoStoreDetailParser.parse(Jsoup.parse("<html></html>"));

        assertThat(result.imageUrl()).isNull();
        assertThat(result.businessHours()).isNull();
        assertThat(result.openStatus()).isNull();
    }
}
