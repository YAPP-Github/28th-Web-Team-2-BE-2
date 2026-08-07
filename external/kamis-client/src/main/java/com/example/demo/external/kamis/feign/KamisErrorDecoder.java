package com.example.demo.external.kamis.feign;

import com.example.demo.external.kamis.KamisDailyPriceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;

public final class KamisErrorDecoder implements ErrorDecoder {

    private static final String DEFAULT_ERROR_MESSAGE = "KAMIS API 호출에 실패했습니다.";

    private final ObjectMapper objectMapper;

    public KamisErrorDecoder(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return new KamisClientException(response.status(), readErrorMessage(response));
    }

    private String readErrorMessage(final Response response) {
        if (response.body() == null) {
            return DEFAULT_ERROR_MESSAGE;
        }

        try (InputStream body = response.body().asInputStream()) {
            final KamisDailyPriceResponse errorResponse = objectMapper.readValue(body, KamisDailyPriceResponse.class);
            if (errorResponse == null || errorResponse.data() == null
                    || errorResponse.data().errorMessage() == null
                    || errorResponse.data().errorMessage().isBlank()) {
                return DEFAULT_ERROR_MESSAGE;
            }
            return errorResponse.data().errorMessage();
        } catch (IOException | RuntimeException exception) {
            return DEFAULT_ERROR_MESSAGE;
        }
    }
}
