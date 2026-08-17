package com.example.demo.external.kakao.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.Response;
import feign.codec.Decoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.springframework.http.HttpStatus;

public final class KakaoResponseDecoder implements Decoder {

    private static final String META = "meta";
    private static final String TOTAL_COUNT = "total_count";
    private static final String DOCUMENTS = "documents";
    private static final String TOTAL_COUNT_CAMEL_CASE = "totalCount";
    private final ObjectMapper objectMapper;

    public KakaoResponseDecoder(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(final Response response, final Type type) throws IOException {
        if (response.body() == null) {
            throw externalApiException();
        }
        try (InputStream body = response.body().asInputStream()) {
            final JsonNode root = objectMapper.readTree(body);
            return objectMapper.convertValue(
                    normalize(root, type), objectMapper.constructType(type));
        } catch (final ApiException exception) {
            throw exception;
        } catch (final IOException | RuntimeException exception) {
            throw externalApiException(exception);
        }
    }

    private ObjectNode normalize(final JsonNode root, final Type type) {
        validateEnvelope(root);
        final JsonNode documents = root.get(DOCUMENTS);
        validateDocuments(documents, type);
        final ObjectNode normalized = root.deepCopy();
        normalized.set(TOTAL_COUNT_CAMEL_CASE, root.get(META).get(TOTAL_COUNT));
        normalized.remove(META);
        if (type == KakaoRegionCodeResult.class) {
            normalized.set("regions", normalizeRegions(documents));
        }
        if (type == KakaoCategorySearchResult.class) {
            normalized.set("places", normalizePlaces(documents));
        }
        normalized.remove(DOCUMENTS);
        return normalized;
    }

    private void validateEnvelope(final JsonNode root) {
        if (root == null || !root.isObject()) {
            throw externalApiException();
        }
        final JsonNode meta = root.get(META);
        final JsonNode documents = root.get(DOCUMENTS);
        if (meta == null || !meta.isObject() || documents == null || !documents.isArray()) {
            throw externalApiException();
        }
        final JsonNode totalCount = meta.get(TOTAL_COUNT);
        if (totalCount == null || !totalCount.isIntegralNumber() || totalCount.longValue() < 0) {
            throw externalApiException();
        }
    }

    private void validateDocuments(final JsonNode documents, final Type type) {
        documents.forEach(document -> validateDocument(document, type));
    }

    private void validateDocument(final JsonNode document, final Type type) {
        if (!document.isObject()) {
            throw externalApiException();
        }
        if (type == KakaoRegionCodeResult.class) {
            validateRegion(document);
            return;
        }
        if (type == KakaoCategorySearchResult.class) {
            validatePlace(document);
        }
    }

    private void validateRegion(final JsonNode region) {
        requireText(region, "region_type");
        requireLong(region, "code");
        requireText(region, "region_2depth_name");
        requireText(region, "region_3depth_name");
    }

    private void validatePlace(final JsonNode place) {
        requireText(place, "id");
        requireText(place, "place_name");
        requireDecimal(place, "x");
        requireDecimal(place, "y");
        requireText(place, "address_name");
        requireText(place, "road_address_name");
        requireText(place, "phone");
        requireText(place, "place_url");
        requireInteger(place, "distance");
    }

    private void requireText(final JsonNode document, final String field) {
        final JsonNode value = document.get(field);
        if (value == null || !value.isTextual()) {
            throw externalApiException();
        }
    }

    private void requireLong(final JsonNode document, final String field) {
        final JsonNode value = document.get(field);
        if (!isTextualNumber(value, Long::parseLong)) {
            throw externalApiException();
        }
    }

    private void requireDecimal(final JsonNode document, final String field) {
        final JsonNode value = document.get(field);
        if (!isTextualNumber(value, java.math.BigDecimal::new)) {
            throw externalApiException();
        }
    }

    private void requireInteger(final JsonNode document, final String field) {
        final JsonNode value = document.get(field);
        if (!isTextualNumber(value, Integer::parseInt)) {
            throw externalApiException();
        }
    }

    private boolean isTextualNumber(final JsonNode value, final NumberParser parser) {
        if (value == null || !value.isTextual()) {
            return false;
        }
        try {
            parser.parse(value.textValue());
            return true;
        } catch (final NumberFormatException exception) {
            return false;
        }
    }

    private JsonNode normalizeRegions(final JsonNode documents) {
        final var regions = objectMapper.createArrayNode();
        documents.forEach(document -> {
            final ObjectNode region = document.deepCopy();
            region.remove("address_name");
            region.remove("region_1depth_name");
            region.remove("region_4depth_name");
            region.remove("x");
            region.remove("y");
            region.set("regionType", region.remove("region_type"));
            region.set("region2DepthName", region.remove("region_2depth_name"));
            region.set("region3DepthName", region.remove("region_3depth_name"));
            regions.add(region);
        });
        return regions;
    }

    private JsonNode normalizePlaces(final JsonNode documents) {
        final var places = objectMapper.createArrayNode();
        documents.forEach(document -> {
            final ObjectNode place = document.deepCopy();
            place.remove("category_name");
            place.remove("category_group_code");
            place.remove("category_group_name");
            place.set("placeName", place.remove("place_name"));
            place.set("longitude", place.remove("x"));
            place.set("latitude", place.remove("y"));
            place.set("addressName", place.remove("address_name"));
            place.set("roadAddressName", place.remove("road_address_name"));
            place.set("placeUrl", place.remove("place_url"));
            place.set("distanceMeters", place.remove("distance"));
            places.add(place);
        });
        return places;
    }

    private ApiException externalApiException() {
        return new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY);
    }

    private ApiException externalApiException(final Exception exception) {
        return new ApiException(
                ErrorType.EXTERNAL_API_ERROR.description(),
                ErrorType.EXTERNAL_API_ERROR,
                HttpStatus.BAD_GATEWAY,
                exception);
    }

    @FunctionalInterface
    private interface NumberParser {

        Number parse(String value) throws NumberFormatException;
    }
}
