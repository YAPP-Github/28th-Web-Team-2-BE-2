package com.example.demo.external.kamis.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kamis.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public final class KamisErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    public KamisErrorDecoder(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(final String methodKey, final Response response) {
        final HttpStatus httpStatus = Optional.ofNullable(HttpStatus.resolve(response.status()))
                .orElse(HttpStatus.BAD_GATEWAY);
        if (response.body() == null) {
            return new ApiException(
                    ErrorType.EXTERNAL_API_ERROR.description(),
                    ErrorType.EXTERNAL_API_ERROR,
                    httpStatus);
        }

        try (InputStream body = response.body().asInputStream()) {
            final ErrorResponse errorResponse = objectMapper.readValue(body, ErrorResponse.class);
            return new ApiException(
                    readErrorMessage(errorResponse),
                    ErrorType.EXTERNAL_API_ERROR,
                    httpStatus);
        } catch (IOException exception) {
            log.error(
                    "[KAMIS] response parsing failed status={} methodKey={}",
                    response.status(),
                    methodKey,
                    exception);
            return new ApiException(
                    "KAMIS API 응답 파싱 실패",
                    ErrorType.EXTERNAL_API_ERROR,
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private String readErrorMessage(final ErrorResponse errorResponse) {
        if (errorResponse == null || errorResponse.openApiServiceResponse() == null
                || errorResponse.openApiServiceResponse().cmmMsgHeader() == null) {
            return ErrorType.EXTERNAL_API_ERROR.description();
        }

        final ErrorResponse.CmmMsgHeader header =
                errorResponse.openApiServiceResponse().cmmMsgHeader();
        if (header.returnAuthMsg() != null && !header.returnAuthMsg().isBlank()) {
            return header.returnAuthMsg();
        }
        if (header.errMsg() != null && !header.errMsg().isBlank()) {
            return header.errMsg();
        }
        return ErrorType.EXTERNAL_API_ERROR.description();
    }

}
