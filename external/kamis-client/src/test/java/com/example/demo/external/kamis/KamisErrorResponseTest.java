package com.example.demo.external.kamis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class KamisErrorResponseTest {

    @Test
    void KAMIS_오류_응답을_전용_모델로_변환한다() throws IOException {
        final KamisErrorResponse response = new ObjectMapper().readValue(
                "{\"OpenAPI_ServiceResponse\":{\"cmmMsgHeader\":{"
                        + "\"errMsg\":\"SERVICE_KEY_IS_NOT_REGISTERED_ERROR\","
                        + "\"returnAuthMsg\":\"등록되지 않은 서비스키\","
                        + "\"returnReasonCode\":\"30\"}}}",
                KamisErrorResponse.class);

        assertThat(response.openApiServiceResponse().cmmMsgHeader().errMsg())
                .isEqualTo("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
        assertThat(response.openApiServiceResponse().cmmMsgHeader().returnAuthMsg())
                .isEqualTo("등록되지 않은 서비스키");
        assertThat(response.openApiServiceResponse().cmmMsgHeader().returnReasonCode())
                .isEqualTo("30");
    }
}
