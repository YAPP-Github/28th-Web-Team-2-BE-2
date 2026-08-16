package com.example.demo.external.kakao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class KakaoLocalClientContractTest {

    @Test
    void Kakao_Local_클라이언트는_FeignClient로_선언된다() {
        assertThat(KakaoLocalClient.class.getAnnotation(FeignClient.class)).isNotNull();
    }

    @Test
    void Kakao_지역_코드_결과는_법정동만_제공한다() {
        final var result = new KakaoRegionCodeResult(
                2,
                List.of(
                        new KakaoRegion("B", 4413310500L, "천안시 서북구", "성성동"),
                        new KakaoRegion("H", 4413357000L, "천안시 서북구", "부성2동")));

        assertThat(result.legalRegions())
                .singleElement()
                .isEqualTo(new KakaoRegion("B", 4413310500L, "천안시 서북구", "성성동"));
    }
}
