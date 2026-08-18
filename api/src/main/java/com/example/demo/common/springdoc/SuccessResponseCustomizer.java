package com.example.demo.common.springdoc;

import com.example.demo.common.presentation.ApiResponse;
import com.example.demo.common.presentation.DirectResponse;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Arrays;
import java.util.stream.Stream;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

@Component
public class SuccessResponseCustomizer implements OperationCustomizer {

    private static final String API_V1_PATH = "/api/v1";

    @Override
    public Operation customize(final Operation operation, final HandlerMethod handlerMethod) {
        if (isApiV1Controller(handlerMethod)
                && !handlerMethod.hasMethodAnnotation(DirectResponse.class)) {
            wrapSuccessResponseSchemas(operation.getResponses());
        }
        return operation;
    }

    private void wrapSuccessResponseSchemas(final ApiResponses responses) {
        if (responses == null) {
            return;
        }
        responses.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("2") || entry.getKey().equals("default"))
                .forEach(entry -> {
                    if (entry.getValue().getContent() != null) {
                        entry.getValue().getContent().forEach((mediaTypeKey, mediaType) -> {
                            final Schema<?> wrappedSchema = new Schema<>();
                            wrappedSchema.addProperty(
                                    "code", new StringSchema().example(ApiResponse.SUCCESS_CODE));
                            wrappedSchema.addProperty(
                                    "message", new StringSchema().example(ApiResponse.SUCCESS_MESSAGE));
                            wrappedSchema.addProperty("data", mediaType.getSchema());
                            mediaType.setSchema(wrappedSchema);
                        });
                    }
                });
    }

    private boolean isApiV1Controller(final HandlerMethod handlerMethod) {
        final RequestMapping mapping = handlerMethod.getBeanType().getAnnotation(RequestMapping.class);
        return mapping != null
                && Stream.concat(Arrays.stream(mapping.value()), Arrays.stream(mapping.path()))
                        .anyMatch(this::isApiV1Path);
    }

    private boolean isApiV1Path(final String path) {
        return path.equals(API_V1_PATH) || path.startsWith(API_V1_PATH + "/");
    }
}
