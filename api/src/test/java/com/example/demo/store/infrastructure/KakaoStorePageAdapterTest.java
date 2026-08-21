package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.store.application.result.StorePageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class KakaoStorePageAdapterTest {

    @Test
    void Kakao_장소_페이지의_절대_OG_이미지를_반환한다() {
        final String imageUrl = adapter("""
                <html><head>
                    <meta property="og:image" content="https://img1.kakaocdn.net/store.jpg">
                </head></html>
                """).findOgImage("http://place.map.kakao.com/123");

        assertThat(imageUrl).isEqualTo("https://img1.kakaocdn.net/store.jpg");
    }

    @Test
    void 프로토콜_상대_OG_이미지는_https로_정규화한다() {
        final String imageUrl = adapter("""
                <html><head>
                    <meta property="og:image" content="//img1.kakaocdn.net/store.jpg">
                </head></html>
                """).findOgImage("https://place.map.kakao.com/123");

        assertThat(imageUrl).isEqualTo("https://img1.kakaocdn.net/store.jpg");
    }

    @Test
    void 허용되지_않은_페이지_URL은_외부_요청_없이_null을_반환한다() {
        final KakaoStorePageAdapter adapter = new KakaoStorePageAdapter(url -> {
            throw new AssertionError("허용되지 않은 URL은 요청하면 안 됩니다");
        });

        final String imageUrl = adapter.findOgImage("https://example.com/123");

        assertThat(imageUrl).isNull();
    }

    @Test
    void Kakao_호스트여도_허용되지_않은_URL_구성요소는_요청하지_않는다() {
        final KakaoStorePageAdapter adapter = new KakaoStorePageAdapter(url -> {
            throw new AssertionError("허용되지 않은 URL은 요청하면 안 됩니다");
        });

        assertThat(adapter.findOgImage("https://user@place.map.kakao.com/123")).isNull();
        assertThat(adapter.findOgImage("https://place.map.kakao.com:8443/123")).isNull();
        assertThat(adapter.findOgImage("https://place.map.kakao.com/123?preview=true")).isNull();
        assertThat(adapter.findOgImage("https://place.map.kakao.com/123#image")).isNull();
        assertThat(adapter.findOgImage("https://place.map.kakao.com/main/v/123")).isNull();
    }

    @Test
    void 페이지_조회_실패는_상세_응답을_막지_않도록_null을_반환한다() {
        final KakaoStorePageAdapter adapter = new KakaoStorePageAdapter(url -> {
            throw new IllegalStateException("fetch failed");
        });

        assertThat(adapter.findOgImage("https://place.map.kakao.com/123")).isNull();
    }

    @Test
    void OG_이미지가_없거나_지원하지_않는_URL이면_null을_반환한다() {
        assertThat(adapter("<html></html>")
                .findOgImage("https://place.map.kakao.com/123"))
                .isNull();
        assertThat(adapter("""
                <html><head>
                    <meta property="og:image" content="javascript:alert(1)">
                </head></html>
                """).findOgImage("https://place.map.kakao.com/123"))
                .isNull();
    }

    @Test
    void 허용되지_않은_이미지_호스트는_null을_반환한다() {
        assertThat(adapter("""
                <html><head>
                    <meta property="og:image" content="https://img.example.com/store.jpg">
                </head></html>
                """).findOgImage("https://place.map.kakao.com/123"))
                .isNull();
    }

    @Test
    void 장소_페이지에서_영업시간_상태와_이미지_바이트를_수집한다() {
        final byte[] png = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        final KakaoStorePageAdapter adapter = new KakaoStorePageAdapter(
                url -> document("""
                        <html><head>
                            <meta property="og:image" content="https://img1.kakaocdn.net/store.png">
                        </head><body>
                            <div class="info_operation">
                                <span class="txt_status">영업중</span>
                                <ul class="list_operation">
                                    <li><span class="day">월</span><span class="time">09:00 ~ 18:00</span></li>
                                    <li><span class="day">화</span><span class="time">09:00 ~ 18:00</span></li>
                                    <li><span class="day">수</span><span class="time">휴무</span></li>
                                </ul>
                            </div>
                        </body></html>
                        """),
                url -> new KakaoStorePageAdapter.DownloadedImage("image/png", png));

        final StorePageContent content = adapter.find("https://place.map.kakao.com/123");

        assertThat(content.imageUrl()).isEqualTo("https://img1.kakaocdn.net/store.png");
        assertThat(content.imageContentType()).isEqualTo("image/png");
        assertThat(content.imageContent()).containsExactly(png);
        assertThat(content.businessHours())
                .containsExactly("월 09:00 ~ 18:00", "화 09:00 ~ 18:00", "수 휴무");
        assertThat(content.openStatus()).isEqualTo("OPEN");
    }

    private KakaoStorePageAdapter adapter(final String html) {
        return new KakaoStorePageAdapter(url -> document(html));
    }

    private Document document(final String html) {
        return Jsoup.parse(html, "https://place.map.kakao.com/123");
    }
}
