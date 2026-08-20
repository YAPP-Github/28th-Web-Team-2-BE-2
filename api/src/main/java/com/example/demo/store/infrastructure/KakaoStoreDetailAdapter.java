package com.example.demo.store.infrastructure;

import com.example.demo.store.application.port.ImageStoragePort;
import com.example.demo.store.application.port.StoreDetailEnrichmentPort;
import com.example.demo.store.application.result.StoreDetailSnapshot;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoStoreDetailAdapter implements StoreDetailEnrichmentPort {

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_HOSTS = List.of("place.map.kakao.com", "map.kakao.com");

    private final ImageStoragePort imageStoragePort;

    @Override
    public StoreDetailSnapshot enrich(final StoreDetailSnapshot snapshot) {
        if (snapshot.placeUrl() == null || snapshot.placeUrl().isBlank()) {
            return snapshot;
        }
        final String detailUrl = normalizeKakaoUrl(snapshot.placeUrl());
        try {
            final Connection.Response response = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .maxBodySize(2 * 1024 * 1024)
                    .followRedirects(false)
                    .execute();
            ensureSuccessful(response);
            final Document document = response.parse();
            final KakaoStoreDetailParserResult parsed = KakaoStoreDetailParser.parse(document);
            final String imageUrl = uploadOrgImage(parsed.imageUrl());
            return new StoreDetailSnapshot(
                    snapshot.storeId(), snapshot.storeName(), snapshot.address(), snapshot.regionId(),
                    snapshot.regionName(), snapshot.latitude(), snapshot.longitude(), detailUrl, imageUrl,
                    parsed.businessHours(), parsed.openStatus(), snapshot.kakaoDetailsCollectedAt());
        } catch (final Exception exception) {
            return snapshot;
        }
    }

    private String uploadOrgImage(final String imageUrl) throws Exception {
        if (imageUrl == null) {
            return null;
        }
        if (imageUrl.isBlank()) {
            return null;
        }
        final String normalizedImageUrl = normalizeKakaoUrl(imageUrl);
        final Connection.Response imageResponse = Jsoup.connect(normalizedImageUrl)
                .ignoreContentType(true)
                .timeout(5000)
                .maxBodySize(MAX_IMAGE_BYTES + 1)
                .followRedirects(false)
                .execute();
        ensureSuccessful(imageResponse);
        final byte[] bytes = imageResponse.bodyAsBytes();
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            return null;
        }
        final String contentType = imageResponse.contentType();
        final String extension = extension(contentType);
        if (extension == null) {
            return null;
        }
        return imageStoragePort.upload(bytes, contentType, extension);
    }

    private void ensureSuccessful(final Connection.Response response) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected Kakao response status: " + response.statusCode());
        }
    }


    private String normalizeKakaoUrl(final String url) {
        final URI uri = URI.create(url);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || !ALLOWED_HOSTS.contains(uri.getHost())) {
            throw new IllegalArgumentException("Unsupported Kakao URL");
        }
        return "https".equalsIgnoreCase(uri.getScheme())
                ? url
                : URI.create("https" + url.substring(uri.getScheme().length())).toString();
    }

    private String extension(final String contentType) {
        if (contentType == null) {
            return null;
        }
        if (contentType.startsWith("image/png")) {
            return "png";
        }
        if (contentType.startsWith("image/jpeg")) {
            return "jpg";
        }
        return null;
    }
}
