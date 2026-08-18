package com.example.demo.external.kakao.feign;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoAddressSearchResult;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class KakaoMapClientFeignContractTest {

    @Test
    void Kakao_Map_클라이언트의_Feign_설정을_선언한다() {
        final FeignClient annotation = KakaoMapClient.class.getAnnotation(FeignClient.class);
        final Map<String, Class<?>> returnTypes = Arrays.stream(KakaoMapClient.class.getDeclaredMethods())
                .collect(Collectors.toMap(Method::getName, Method::getReturnType));

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("kakaoMapClient");
        assertThat(annotation.url()).isEqualTo("${kakao.map.url:https://dapi.kakao.com}");
        assertThat(annotation.configuration()).containsExactly(KakaoMapClientConfiguration.class);
        assertThat(returnTypes)
                .containsEntry("searchCategory", KakaoCategorySearchResult.class)
                .containsEntry("searchAddress", KakaoAddressSearchResult.class)
                .containsEntry("searchRegionCode", KakaoRegionCodeResult.class);
    }

    @Test
    void Kakao_REST_API_Key를_Authorization_헤더에_추가한다() {
        final RequestTemplate template = new RequestTemplate();
        final RequestInterceptor interceptor = new KakaoMapClientConfiguration()
                .requestInterceptor("rest-api-key");

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization"))
                .containsExactly("KakaoAK rest-api-key");
    }
}
