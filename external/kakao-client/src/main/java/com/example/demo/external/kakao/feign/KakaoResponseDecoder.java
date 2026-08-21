package com.example.demo.external.kakao.feign;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoAddressSearchResult;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.Response;
import feign.codec.Decoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;

public final class KakaoResponseDecoder implements Decoder {

    private static final String META = "meta";
    private static final String TOTAL_COUNT = "total_count";
    private static final String PAGEABLE_COUNT = "pageable_count";
    private static final long MAX_PAGEABLE_COUNT = 45;
    private static final String DOCUMENTS = "documents";
    private static final String TOTAL_COUNT_CAMEL_CASE = "totalCount";
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
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
        validateEnvelope(root, type);
        final JsonNode documents = validateDocuments(root.get(DOCUMENTS), type);
        final ObjectNode normalized = root.deepCopy();
        final JsonNode meta = root.get(META);
        normalized.set(TOTAL_COUNT_CAMEL_CASE, meta.get(TOTAL_COUNT));
        normalized.remove(META);
        if (type == KakaoRegionCodeResult.class) {
            normalized.set("regions", normalizeRegions(documents));
        }
        if (type == KakaoAddressSearchResult.class) {
            normalized.set("addresses", normalizeAddresses(documents));
        }
        if (type == KakaoCategorySearchResult.class) {
            normalized.set("pageableCount", meta.get(PAGEABLE_COUNT));
            normalized.set("end", meta.get("is_end"));
            normalized.set("places", normalizePlaces(documents));
        }
        normalized.remove(DOCUMENTS);
        return normalized;
    }

    private void validateEnvelope(final JsonNode root, final Type type) {
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
        if (type == KakaoCategorySearchResult.class) {
            final JsonNode pageableCount = meta.get(PAGEABLE_COUNT);
            final JsonNode isEnd = meta.get("is_end");
            if (pageableCount == null
                    || !pageableCount.isIntegralNumber()
                    || pageableCount.longValue() < 0
                    || pageableCount.longValue() > MAX_PAGEABLE_COUNT
                    || pageableCount.longValue() > totalCount.longValue()
                    || isEnd == null
                    || !isEnd.isBoolean()) {
                throw externalApiException();
            }
        }
    }

    private JsonNode validateDocuments(final JsonNode documents, final Type type) {
        final var validDocuments = objectMapper.createArrayNode();
        documents.forEach(document -> {
            if (!document.isObject()) {
                throw externalApiException();
            }
            if (type == KakaoAddressSearchResult.class) {
                if (validateAddress(document)) {
                    validDocuments.add(document);
                }
                return;
            }
            validateDocument(document, type);
            validDocuments.add(document);
        });
        return validDocuments;
    }

    private void validateDocument(final JsonNode document, final Type type) {
        if (!document.isObject()) {
            throw externalApiException();
        }
        if (type == KakaoRegionCodeResult.class) {
            validateRegion(document);
            return;
        }
        if (type == KakaoAddressSearchResult.class) {
            validateAddress(document);
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

    private boolean validateAddress(final JsonNode address) {
        requireNonBlankText(address, "address_name");
        requireNonBlankText(address, "address_type");
        validateCoordinates(address);
        final JsonNode detail = address.get("address");
        if (detail == null) {
            throw externalApiException();
        }
        if (detail.isNull()) {
            return false;
        }
        if (!detail.isObject()) {
            throw externalApiException();
        }
        requireText(detail, "region_3depth_name");
        requireNonBlankText(detail, "address_name");
        requireNonBlankText(detail, "region_1depth_name");
        requireNonBlankText(detail, "region_2depth_name");
        requireText(detail, "b_code");
        if (detail.get("region_3depth_name").textValue().isBlank()) {
            return false;
        }
        return true;
    }

    private void validateCoordinates(final JsonNode address) {
        final BigDecimal longitude = requireDecimal(address, "x");
        if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw externalApiException();
        }
        final BigDecimal latitude = requireDecimal(address, "y");
        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw externalApiException();
        }
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

    private void requireNonBlankText(final JsonNode document, final String field) {
        requireText(document, field);
        if (document.get(field).textValue().isBlank()) {
            throw externalApiException();
        }
    }

    private void requireLong(final JsonNode document, final String field) {
        final JsonNode value = document.get(field);
        if (!isTextualNumber(value, Long::parseLong)) {
            throw externalApiException();
        }
    }

    private BigDecimal requireDecimal(final JsonNode document, final String field) {
        final JsonNode value = document.get(field);
        if (!isTextualNumber(value, BigDecimal::new)) {
            throw externalApiException();
        }
        return new BigDecimal(value.textValue());
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

    private JsonNode normalizeAddresses(final JsonNode documents) {
        final var addresses = objectMapper.createArrayNode();
        documents.forEach(document -> {
            final ObjectNode address = document.deepCopy();
            address.set("addressName", address.remove("address_name"));
            address.set("addressType", address.remove("address_type"));
            address.set("longitude", address.remove("x"));
            address.set("latitude", address.remove("y"));
            address.remove("road_address");
            final ObjectNode sourceDetail = (ObjectNode) address.get("address");
            final ObjectNode detail = objectMapper.createObjectNode();
            detail.set("addressName", sourceDetail.get("address_name"));
            detail.set("region1DepthName", sourceDetail.get("region_1depth_name"));
            detail.set("region2DepthName", sourceDetail.get("region_2depth_name"));
            detail.set("region3DepthName", sourceDetail.get("region_3depth_name"));
            detail.set("bCode", sourceDetail.get("b_code"));
            address.set("address", detail);
            addresses.add(address);
        });
        return addresses;
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
