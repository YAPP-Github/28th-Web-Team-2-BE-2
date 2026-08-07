package com.example.demo.external.kamis;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

class KamisClientFeignContractTest {

    @Test
    void KAMIS_클라이언트는_FeignClient로_선언된다() {
        assertThat(KamisClient.class.getAnnotation(FeignClient.class)).isNotNull();
    }

    @Test
    void KAMIS_인증값을_요청_쿼리에_추가한다() {
        final RequestTemplate template = new RequestTemplate();
        final RequestInterceptor interceptor = new KamisClientConfiguration()
                .requestInterceptor("cert-key-for-test", "9220");

        interceptor.apply(template);

        assertThat(template.queries().get("p_cert_key")).containsExactly("cert-key-for-test");
        assertThat(template.queries().get("p_cert_id")).containsExactly("9220");
    }
}
