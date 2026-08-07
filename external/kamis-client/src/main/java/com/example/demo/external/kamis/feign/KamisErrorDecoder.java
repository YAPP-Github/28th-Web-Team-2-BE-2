package com.example.demo.external.kamis.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kamis.KamisErrorResponse;
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
            final KamisErrorResponse errorResponse = objectMapper.readValue(body, KamisErrorResponse.class);
            if (errorResponse == null || errorResponse.openApiServiceResponse() == null
                    || errorResponse.openApiServiceResponse().cmmMsgHeader() == null) {
                return ErrorType.EXTERNAL_API_ERROR.description();
            }
            final KamisErrorResponse.CmmMsgHeader header =
                    errorResponse.openApiServiceResponse().cmmMsgHeader();
            if (header.returnAuthMsg() != null && !header.returnAuthMsg().isBlank()) {
                return header.returnAuthMsg();
            }
            if (header.errMsg() != null && !header.errMsg().isBlank()) {
                return header.errMsg();
            }
            return ErrorType.EXTERNAL_API_ERROR.description();
        } catch (IOException | RuntimeException exception) {
            return ErrorType.EXTERNAL_API_ERROR.description();
        }
    }
}
