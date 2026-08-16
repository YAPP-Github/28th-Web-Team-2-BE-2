package com.example.demo.external.kakao;

import com.example.demo.external.kakao.feign.KakaoMapClientConfiguration;
import java.math.BigDecimal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "kakaoMapClient",
        url = "${kakao.map.url:https://dapi.kakao.com}",
        configuration = KakaoMapClientConfiguration.class)
public interface KakaoMapClient {

    @GetMapping("/v2/local/search/category.json")
    KakaoCategorySearchResult searchCategory(
            @RequestParam("category_group_code") String categoryGroupCode,
            @RequestParam("x") BigDecimal longitude,
            @RequestParam("y") BigDecimal latitude,
            @RequestParam("radius") int radius,
            @RequestParam("sort") String sort,
            @RequestParam("size") int size);

    @GetMapping("/v2/local/geo/coord2regioncode.json")
    KakaoRegionCodeResult searchRegionCode(
            @RequestParam("x") BigDecimal longitude,
            @RequestParam("y") BigDecimal latitude);
}
