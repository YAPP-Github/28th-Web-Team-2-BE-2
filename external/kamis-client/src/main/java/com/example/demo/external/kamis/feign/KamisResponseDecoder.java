package com.example.demo.external.kamis.feign;

import com.example.demo.external.kamis.DailyPriceResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.Response;
import feign.codec.Decoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public final class KamisResponseDecoder implements Decoder {

    private static final String DATA = "data";
    private static final String RESPONSE = "response";
    private static final String BODY = "body";
    private static final String ITEM = "item";
    private static final String ITEMS = "items";
    private static final String META = "meta";
    private static final String DATA_TYPE = "dataType";
    private static final String NUM_OF_ROWS = "numOfRows";
    private static final String PAGE_NO = "pageNo";
    private static final String TOTAL_COUNT = "totalCount";

    private final ObjectMapper objectMapper;

    public KamisResponseDecoder(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(final Response response, final Type type) throws IOException {
        if (response.body() == null) {
            return null;
        }
        try (InputStream body = response.body().asInputStream()) {
            final JsonNode root = objectMapper.readTree(body);
            final JsonNode normalized = normalize(root);
            return objectMapper.convertValue(normalized, objectMapper.constructType(type));
        }
    }

    private JsonNode normalize(final JsonNode root) {
        JsonNode source = root;
        if (root.has(DATA)) {
            source = root.get(DATA);
        }
        if (root.has(RESPONSE)) {
            source = root.path(RESPONSE).path(BODY);
        }
        if (!source.has(ITEMS)) {
            return source;
        }

        final ObjectNode normalized = source.deepCopy();
        final JsonNode rawItems = source.path(ITEMS);
        final JsonNode items = rawItems.isArray() ? rawItems : rawItems.path(ITEM);
        if (items.isArray()) {
            normalized.set(ITEM, items);
        }
        normalized.remove(ITEMS);
        copyMetaFields(source, normalized);
        return normalized;
    }

    private void copyMetaFields(final JsonNode source, final ObjectNode normalized) {
        if (source.has(META)) {
            return;
        }
        final ObjectNode meta = objectMapper.createObjectNode();
        copyField(source, meta, DATA_TYPE);
        copyField(source, meta, NUM_OF_ROWS);
        copyField(source, meta, PAGE_NO);
        copyField(source, meta, TOTAL_COUNT);
        if (!meta.isEmpty()) {
            normalized.set(META, meta);
        }
    }

    private void copyField(final JsonNode source, final ObjectNode target, final String field) {
        if (source.has(field)) {
            target.set(field, source.get(field));
        }
    }
}
