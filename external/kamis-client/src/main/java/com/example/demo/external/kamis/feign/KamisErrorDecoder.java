package com.example.demo.external.kamis.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kamis.KamisDailyPriceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpStatus;

public final class KamisErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    public KamisErrorDecoder(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return new ApiException(
                readErrorMessage(response),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.valueOf(response.status()));
    }

    private String readErrorMessage(final Response response) {
        if (response.body() == null) {
            return ErrorType.EXTERNAL_API_ERROR.description();
        }

        try (InputStream body = response.body().asInputStream()) {
            final KamisDailyPriceResponse errorResponse = objectMapper.readValue(body, KamisDailyPriceResponse.class);
            if (errorResponse == null || errorResponse.data() == null
                    || errorResponse.data().errorMessage() == null
                    || errorResponse.data().errorMessage().isBlank()) {
                return ErrorType.EXTERNAL_API_ERROR.description();
            }
            return errorResponse.data().errorMessage();
        } catch (IOException | RuntimeException exception) {
            return ErrorType.EXTERNAL_API_ERROR.description();
        }
    }
}
