package com.example.demo.external.kamis.feign;

import com.example.demo.external.kamis.DailyPriceResponse;
import com.example.demo.external.kamis.WholesalePeriodPriceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "kamisClient",
        url = "${kamis.url}",
        configuration = KamisClientConfiguration.class)
public interface KamisClient {

    @GetMapping("/xml.do")
    DailyPriceResponse getDailyPrices(
            @RequestParam("action") String action,
            @RequestParam("p_product_cls_code") String productClsCode,
            @RequestParam("p_item_category_code") String itemCategoryCode,
            @RequestParam(value = "p_country_code", required = false) String countryCode,
            @RequestParam(value = "p_regday", required = false) String regDay,
            @RequestParam("p_convert_kg_yn") String convertKgYn,
            @RequestParam("p_returntype") String returnType);

    @GetMapping("/xml.do")
    WholesalePeriodPriceResponse getWholesalePeriodPrices(
            @RequestParam("action") String action,
            @RequestParam("p_startday") String startDay,
            @RequestParam("p_endday") String endDay,
            @RequestParam("p_itemcategorycode") String itemCategoryCode,
            @RequestParam("p_itemcode") String itemCode,
            @RequestParam(value = "p_kindcode", required = false) String kindCode,
            @RequestParam("p_productrankcode") String productRankCode,
            @RequestParam("p_countrycode") String countryCode,
            @RequestParam("p_convert_kg_yn") String convertKgYn,
            @RequestParam("p_returntype") String returnType);
}
