package com.example.demo.region.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.external.kakao.KakaoAddressSearchResult;
import com.example.demo.external.kakao.KakaoAddressSearchResult.Address;
import com.example.demo.external.kakao.KakaoAddressSearchResult.KakaoAddress;
import com.example.demo.external.kakao.feign.KakaoMapClient;
import com.example.demo.region.application.query.RegionSearchQuery;
import java.util.List;
import org.junit.jupiter.api.Test;

class KakaoRegionSearchAdapterTest {

    private final KakaoMapClient kakaoMapClient = mock(KakaoMapClient.class);
    private final KakaoRegionSearchAdapter adapter = new KakaoRegionSearchAdapter(kakaoMapClient);

    @Test
    void 주소_검색_결과를_법정동_검색_결과로_변환하고_중복_코드를_제거한다() {
        when(kakaoMapClient.searchAddress("성성", 30))
                .thenReturn(new KakaoAddressSearchResult(3, List.of(
                        address("0111010100", "서울특별시", "종로구", "청운동"),
                        address("0111010100", "서울특별시", "종로구", "청운동"),
                        address("4413310500", "충청남도", "천안시 서북구", "성성동"))));

        final var result = adapter.search(new RegionSearchQuery("성성"));

        assertThat(result.regions())
                .extracting("regionId", "regionName")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("0111010100", "서울특별시 종로구 청운동"),
                        org.assertj.core.groups.Tuple.tuple("4413310500", "충청남도 천안시 서북구 성성동"));
    }

    @Test
    void 주소_검색_결과가_없으면_빈_결과를_반환한다() {
        when(kakaoMapClient.searchAddress("없는동", 30))
                .thenReturn(new KakaoAddressSearchResult(0, List.of()));

        assertThat(adapter.search(new RegionSearchQuery("없는동")).regions()).isEmpty();
    }

    private KakaoAddress address(
            final String code,
            final String firstDepth,
            final String secondDepth,
            final String thirdDepth) {
        return new KakaoAddress(
                firstDepth + " " + secondDepth + " " + thirdDepth,
                "REGION",
                new Address(
                        firstDepth + " " + secondDepth + " " + thirdDepth,
                        firstDepth,
                        secondDepth,
                        thirdDepth,
                        code));
    }
}
