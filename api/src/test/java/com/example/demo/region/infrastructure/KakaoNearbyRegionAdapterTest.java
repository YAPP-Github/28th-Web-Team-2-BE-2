package com.example.demo.region.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kakao.KakaoClientException;
import com.example.demo.external.kakao.KakaoLocalClient;
import com.example.demo.external.kakao.KakaoRegion;
import com.example.demo.external.kakao.KakaoRegionCodeQuery;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.example.demo.region.application.query.NearbyRegionQuery;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class KakaoNearbyRegionAdapterTest {

    private final KakaoLocalClient kakaoLocalClient = mock(KakaoLocalClient.class);
    private final KakaoNearbyRegionAdapter adapter = new KakaoNearbyRegionAdapter(kakaoLocalClient);

    @Test
    void Kakao_좌표_행정구역_검색에서_법정동만_변환한다() {
        when(kakaoLocalClient.searchRegionCode(any(KakaoRegionCodeQuery.class)))
                .thenReturn(new KakaoRegionCodeResult(
                        2,
                        List.of(
                                new KakaoRegion("B", "4413310500", "천안시 서북구", "성성동"),
                                new KakaoRegion("H", "4413357000", "천안시 서북구", "부성2동"))));

        final var result = adapter.find(new NearbyRegionQuery(
                new BigDecimal("36.8358"), new BigDecimal("127.1324")));

        assertThat(result.regions())
                .singleElement()
                .satisfies(region -> {
                    assertThat(region.regionId()).isEqualTo("4413310500");
                    assertThat(region.regionName()).isEqualTo("천안시 서북구 성성동");
                });
    }

    @Test
    void Kakao_외부_호출_오류를_외부_API_오류로_변환한다() {
        when(kakaoLocalClient.searchRegionCode(any(KakaoRegionCodeQuery.class)))
                .thenThrow(new KakaoClientException(new IllegalStateException("Kakao failed")));

        assertThatThrownBy(() -> adapter.find(new NearbyRegionQuery(
                        new BigDecimal("36.8358"), new BigDecimal("127.1324"))))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }
}
