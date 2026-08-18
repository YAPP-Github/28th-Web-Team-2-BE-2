package com.example.demo.common.springdoc;

import com.example.demo.common.presentation.ApiResponse;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Arrays;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

@Component
public class SuccessResponseCustomizer implements OperationCustomizer {

    private static final String ITEMS_PATH = "/api/v1/items";

    @Override
    public Operation customize(final Operation operation, final HandlerMethod handlerMethod) {
        if (isItemsController(handlerMethod)) {
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

    private boolean isItemsController(final HandlerMethod handlerMethod) {
        final RequestMapping mapping = handlerMethod.getBeanType().getAnnotation(RequestMapping.class);
        return mapping != null
                && (Arrays.asList(mapping.value()).contains(ITEMS_PATH)
                        || Arrays.asList(mapping.path()).contains(ITEMS_PATH));
    }
}
