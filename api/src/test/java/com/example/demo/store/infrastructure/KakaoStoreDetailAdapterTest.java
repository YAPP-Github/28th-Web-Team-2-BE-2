package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.demo.store.application.port.ImageStoragePort;
import com.example.demo.store.application.result.StoreDetailSnapshot;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KakaoStoreDetailAdapterTest {

    private final KakaoStoreDetailAdapter adapter = new KakaoStoreDetailAdapter(mock(ImageStoragePort.class));

    @Test
    void 장소_URL이_없으면_기존_상세를_그대로_반환한다() {
        final StoreDetailSnapshot snapshot = new StoreDetailSnapshot(
                1L, "가게", "주소", new BigDecimal("37.5"), new BigDecimal("127"));

        assertThat(adapter.enrich(snapshot)).isSameAs(snapshot);
    }

    @Test
    void Kakao가_아닌_URL은_외부_요청하지_않고_거부한다() {
        final StoreDetailSnapshot snapshot = new StoreDetailSnapshot(
                1L, "가게", "주소", new BigDecimal("37.5"), new BigDecimal("127"),
                "https://example.com/store", null, null, "UNKNOWN");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.enrich(snapshot))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
