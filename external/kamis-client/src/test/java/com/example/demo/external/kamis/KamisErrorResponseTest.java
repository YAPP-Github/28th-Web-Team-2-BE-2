package com.example.demo.external.kamis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class KamisErrorResponseTest {

    @Test
    void KAMIS_오류_응답을_전용_모델로_변환한다() throws IOException {
        final KamisErrorResponse response = new ObjectMapper().readValue(
                "{\"data\":{\"error_code\":\"900\",\"error_msg\":\"Unauthenticated request.\"}}",
                KamisErrorResponse.class);

        assertThat(response.data().errorCode()).isEqualTo("900");
        assertThat(response.data().errorMessage()).isEqualTo("Unauthenticated request.");
    }
}
