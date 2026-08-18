package com.example.demo.external.kakao.feign;

import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoAddressSearchResult;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import java.math.BigDecimal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "kakaoMapClient",
        url = "${kakao.map.url:https://dapi.kakao.com}",
        configuration = KakaoMapClientConfiguration.class)
public interface KakaoMapClient {

    @GetMapping("/v2/local/search/address.json")
    KakaoAddressSearchResult searchAddress(
            @RequestParam("query") String query,
            @RequestParam("size") int size);

    @GetMapping("/v2/local/search/category.json")
    KakaoCategorySearchResult searchCategory(
            @RequestParam("category_group_code") String categoryGroupCode,
            @RequestParam("x") BigDecimal longitude,
            @RequestParam("y") BigDecimal latitude,
            @RequestParam("radius") int radius,
            @RequestParam("sort") String sort,
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/v2/local/search/keyword.json")
    KakaoCategorySearchResult searchKeyword(
            @RequestParam("query") String query,
            @RequestParam("category_group_code") String categoryGroupCode,
            @RequestParam("x") BigDecimal longitude,
            @RequestParam("y") BigDecimal latitude,
            @RequestParam("radius") int radius,
            @RequestParam("sort") String sort,
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/v2/local/geo/coord2regioncode.json")
    KakaoRegionCodeResult searchRegionCode(
            @RequestParam("x") BigDecimal longitude,
            @RequestParam("y") BigDecimal latitude);
}
