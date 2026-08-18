package com.example.demo.common.presentation;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ResponseWrapper implements ResponseBodyAdvice<Object> {

    private static final String API_V1_PATH = "/api/v1";

    @Override
    public boolean supports(
            final MethodParameter returnType,
            final Class<? extends HttpMessageConverter<?>> converterType) {
        return AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(
            final Object body,
            final MethodParameter returnType,
            final MediaType selectedContentType,
            final Class<? extends HttpMessageConverter<?>> selectedConverterType,
            final ServerHttpRequest request,
            final ServerHttpResponse response) {
        final String path = request.getURI().getPath();
        if (body == null
                || returnType.hasMethodAnnotation(DirectResponse.class)
                || !isApiV1Path(path)
                || isDocumentationPath(path)
                || body instanceof ApiResponse<?>) {
            return body;
        }
        return new ApiResponse<>(ApiResponse.SUCCESS_CODE, ApiResponse.SUCCESS_MESSAGE, body);
    }

    private boolean isApiV1Path(final String path) {
        return path.equals(API_V1_PATH) || path.startsWith(API_V1_PATH + "/");
    }

    private boolean isDocumentationPath(final String path) {
        return path.contains("/v3/api-docs") || path.contains("/swagger-ui");
    }
}
