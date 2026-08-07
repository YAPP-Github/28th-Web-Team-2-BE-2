package com.example.demo.external.kamis.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kamis.KamisErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public final class KamisErrorDecoder implements ErrorDecoder {

    private static final String PARSING_ERROR_MESSAGE = "KAMIS API 응답 파싱 실패";
    private static final Logger LOGGER = LoggerFactory.getLogger(KamisErrorDecoder.class);

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
            final KamisErrorResponse errorResponse = objectMapper.readValue(body, KamisErrorResponse.class);
            return new ApiException(
                    readErrorMessage(errorResponse),
                    ErrorType.EXTERNAL_API_ERROR,
                    httpStatus);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error(
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

    private String readErrorMessage(final KamisErrorResponse errorResponse) {
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
    }

    private HttpStatus resolveStatus(final int status) {
        final HttpStatus httpStatus = HttpStatus.resolve(status);
        if (httpStatus == null) {
            return HttpStatus.BAD_GATEWAY;
        }
        return httpStatus;
    }
}
