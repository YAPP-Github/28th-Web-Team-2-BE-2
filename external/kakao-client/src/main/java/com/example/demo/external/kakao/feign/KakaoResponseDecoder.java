package com.example.demo.external.kakao.feign;

import com.example.demo.external.kakao.KakaoCategorySearchResult;
import com.example.demo.external.kakao.KakaoClientException;
import com.example.demo.external.kakao.KakaoRegionCodeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.Response;
import feign.codec.Decoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

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
            throw new KakaoClientException(new IllegalStateException("Empty Kakao response"));
        }
        try (InputStream body = response.body().asInputStream()) {
            final JsonNode root = objectMapper.readTree(body);
            return objectMapper.convertValue(
                    normalize(root, type), objectMapper.constructType(type));
        } catch (final IOException | RuntimeException exception) {
            throw new KakaoClientException(exception);
        }
    }

    private ObjectNode normalize(final JsonNode root, final Type type) {
        if (root == null || !root.has(META) || !root.has(DOCUMENTS)) {
            throw new IllegalStateException("Invalid Kakao response");
        }
        final ObjectNode normalized = root.deepCopy();
        normalized.set(TOTAL_COUNT_CAMEL_CASE, root.path(META).path(TOTAL_COUNT));
        normalized.remove(META);
        if (type == KakaoRegionCodeResult.class) {
            normalized.set("regions", normalizeRegions(root.path(DOCUMENTS)));
        }
        if (type == KakaoCategorySearchResult.class) {
            normalized.set("places", normalizePlaces(root.path(DOCUMENTS)));
        }
        normalized.remove(DOCUMENTS);
        return normalized;
    }

    private JsonNode normalizeRegions(final JsonNode documents) {
        final var regions = objectMapper.createArrayNode();
        documents.forEach(document -> {
            final ObjectNode region = document.deepCopy();
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
}
