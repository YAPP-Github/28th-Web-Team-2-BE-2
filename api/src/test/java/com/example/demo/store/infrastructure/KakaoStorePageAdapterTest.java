package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class KakaoStorePageAdapterTest {

    @Test
    void Kakao_장소_페이지의_절대_OG_이미지를_반환한다() {
        final String imageUrl = adapter("""
                <html><head>
                    <meta property="og:image" content="https://img.example.com/store.jpg">
                </head></html>
                """).findOgImage("http://place.map.kakao.com/123");

        assertThat(imageUrl).isEqualTo("https://img.example.com/store.jpg");
    }

    @Test
    void 프로토콜_상대_OG_이미지는_https로_정규화한다() {
        final String imageUrl = adapter("""
                <html><head>
                    <meta property="og:image" content="//img.example.com/store.jpg">
                </head></html>
                """).findOgImage("https://place.map.kakao.com/123");

        assertThat(imageUrl).isEqualTo("https://img.example.com/store.jpg");
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

    private KakaoStorePageAdapter adapter(final String html) {
        return new KakaoStorePageAdapter(url -> document(html));
    }

    private Document document(final String html) {
        return Jsoup.parse(html, "https://place.map.kakao.com/123");
    }
}
