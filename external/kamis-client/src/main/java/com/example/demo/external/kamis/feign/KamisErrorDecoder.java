package com.example.demo.external.kamis.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kamis.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public final class KamisErrorDecoder implements ErrorDecoder {

    private static final String PARSING_ERROR_MESSAGE = "KAMIS API 응답 파싱 실패";

    private final ObjectMapper objectMapper;

    public KamisErrorDecoder(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(final String methodKey, final Response response) {
        final HttpStatus httpStatus = resolveStatus(response.status());
        if (response.body() == null) {
            return new ApiException(
                    ErrorType.EXTERNAL_API_ERROR.description(),
                    ErrorType.EXTERNAL_API_ERROR,
                    httpStatus);
        }

        try (InputStream body = response.body().asInputStream()) {
            final String responseBody = new String(body.readAllBytes(), StandardCharsets.UTF_8);
            final ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
            return new ApiException(
                    readErrorMessage(errorResponse),
                    ErrorType.EXTERNAL_API_ERROR,
                    httpStatus);
        } catch (IOException | RuntimeException exception) {
            log.error(
                    "[Kamis] 응답 파싱 실패 status={} methodKey={} message={}",
                    response.status(),
                    methodKey,
                    exception.getMessage(),
                    exception);
            return new ApiException(
                    PARSING_ERROR_MESSAGE,
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

    private HttpStatus resolveStatus(final int status) {
        final HttpStatus httpStatus = HttpStatus.resolve(status);
        if (httpStatus == null) {
            return HttpStatus.BAD_GATEWAY;
        }
        return httpStatus;
    }
}
