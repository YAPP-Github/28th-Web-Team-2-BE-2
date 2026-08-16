package com.example.demo.region.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.external.kakao.KakaoRegion;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.example.demo.external.kakao.feign.KakaoMapClient;
import com.example.demo.region.application.query.NearbyRegionQuery;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class KakaoNearbyRegionAdapterTest {

    private final KakaoMapClient kakaoMapClient = mock(KakaoMapClient.class);
    private final KakaoNearbyRegionAdapter adapter = new KakaoNearbyRegionAdapter(kakaoMapClient);

    @Test
    void Kakao_좌표_행정구역_검색에서_법정동만_변환한다() {
        when(kakaoMapClient.searchRegionCode(any(), any()))
                .thenReturn(new KakaoRegionCodeResult(
                        2,
                        List.of(
                                new KakaoRegion("B", 4413310500L, "천안시 서북구", "성성동"),
                                new KakaoRegion("H", 4413357000L, "천안시 서북구", "부성2동"))));

        final var result = adapter.find(new NearbyRegionQuery(
                new BigDecimal("36.8358"), new BigDecimal("127.1324")));

        assertThat(result.regions())
                .singleElement()
                .satisfies(region -> {
                    assertThat(region.regionId()).isEqualTo(4413310500L);
                    assertThat(region.regionName()).isEqualTo("천안시 서북구 성성동");
                });
    }
}
