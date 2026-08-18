package com.example.demo.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoPlace;
import com.example.demo.external.kakao.feign.KakaoMapClient;
import com.example.demo.store.application.query.NearbyStoreQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class KakaoNearbyStoreSearchAdapterTest {

    private final KakaoMapClient kakaoMapClient = mock(KakaoMapClient.class);
    private final KakaoNearbyStoreSearchAdapter adapter = new KakaoNearbyStoreSearchAdapter(kakaoMapClient);

    @Test
    void keyword가_있으면_요청_좌표와_반경으로_거리순_keyword_검색을_호출한다() {
        final NearbyStoreQuery query = query("강남 마트");
        when(kakaoMapClient.searchKeyword("강남 마트", "MT1", query.longitude(), query.latitude(),
                1500, "distance", 1, 15))
                .thenReturn(new KakaoCategorySearchResult(1, 1, true, List.of(place("1", 100))));

        assertThat(adapter.search(query).stores()).extracting("kakaoPlaceId").containsExactly("1");
        verify(kakaoMapClient).searchKeyword("강남 마트", "MT1", query.longitude(), query.latitude(),
                1500, "distance", 1, 15);
    }

    @Test
    void 빈_keyword는_기존_category_검색을_호출한다() {
        final NearbyStoreQuery query = query("");
        when(kakaoMapClient.searchCategory("MT1", query.longitude(), query.latitude(), 1500,
                "distance", 1, 15))
                .thenReturn(new KakaoCategorySearchResult(0, 0, true, List.of()));

        assertThat(adapter.search(query).stores()).isEmpty();
        verify(kakaoMapClient).searchCategory("MT1", query.longitude(), query.latitude(), 1500,
                "distance", 1, 15);
    }

    @Test
    void provider_페이지의_중복_place는_502로_실패한다() {
        final NearbyStoreQuery query = query();
        when(kakaoMapClient.searchCategory("MT1", query.longitude(), query.latitude(), 1500,
                "distance", 1, 15))
                .thenReturn(new KakaoCategorySearchResult(3, 3, false, List.of(place("1", 100))));
        when(kakaoMapClient.searchCategory("MT1", query.longitude(), query.latitude(), 1500,
                "distance", 2, 15))
                .thenReturn(new KakaoCategorySearchResult(
                        3, 3, true, List.of(place("1", 100), place("2", 200))));

        assertThatThrownBy(() -> adapter.search(query))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(apiException.httpStatus().value()).isEqualTo(502);
                });
    }

    @Test
    void provider_결과가_45페이지를_초과하면_502로_실패한다() {
        final NearbyStoreQuery query = query();
        when(kakaoMapClient.searchCategory("MT1", query.longitude(), query.latitude(), 1500,
                "distance", 1, 15))
                .thenReturn(new KakaoCategorySearchResult(676, 676, true, List.of()));

        assertThatThrownBy(() -> adapter.search(query))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    final ApiException apiException = (ApiException) exception;
                    assertThat(apiException.errorType()).isEqualTo(ErrorType.EXTERNAL_API_ERROR);
                    assertThat(apiException.httpStatus().value()).isEqualTo(502);
                });
    }

    @Test
    void provider의_total_count가_pageable_count를_초과해도_페이지_상한_내_결과면_성공한다() {
        final NearbyStoreQuery query = query();
        when(kakaoMapClient.searchCategory("MT1", query.longitude(), query.latitude(), 1500,
                "distance", 1, 15))
                .thenReturn(new KakaoCategorySearchResult(100, 45, false, places(1, 16)));
        when(kakaoMapClient.searchCategory("MT1", query.longitude(), query.latitude(), 1500,
                "distance", 2, 15))
                .thenReturn(new KakaoCategorySearchResult(100, 45, false, places(16, 31)));
        when(kakaoMapClient.searchCategory("MT1", query.longitude(), query.latitude(), 1500,
                "distance", 3, 15))
                .thenReturn(new KakaoCategorySearchResult(100, 45, true, places(31, 46)));

        assertThat(adapter.search(query).stores()).hasSize(45);
    }

    private NearbyStoreQuery query() {
        return query(null);
    }

    private NearbyStoreQuery query(final String keyword) {
        return new NearbyStoreQuery(
                new BigDecimal("37.4979"),
                new BigDecimal("127.0276"),
                1500,
                false,
                false,
                null,
                keyword);
    }

    private KakaoPlace place(final String id, final int distance) {
        return new KakaoPlace(
                id,
                "강남마트-" + id,
                new BigDecimal("37.4979"),
                new BigDecimal("127.0276"),
                "서울 강남구 삼성동 123",
                "서울 강남구 테헤란로 123",
                "02-1234-5678",
                "http://place.map.kakao.com/" + id,
                distance);
    }

    private List<KakaoPlace> places(final int startInclusive, final int endExclusive) {
        return IntStream.range(startInclusive, endExclusive)
                .mapToObj(id -> place(String.valueOf(id), id * 100))
                .toList();
    }
}
