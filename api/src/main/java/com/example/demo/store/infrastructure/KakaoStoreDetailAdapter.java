package com.example.demo.store.infrastructure;

import com.example.demo.store.application.port.ImageStoragePort;
import com.example.demo.store.application.port.StoreDetailEnrichmentPort;
import com.example.demo.store.application.result.StoreDetailSnapshot;
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
        validateKakaoUrl(snapshot.placeUrl());
        try {
            final Connection.Response response = Jsoup.connect(snapshot.placeUrl())
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .maxBodySize(2 * 1024 * 1024)
                    .execute();
            final Document document = response.parse();
            final KakaoStoreDetailParserResult parsed = KakaoStoreDetailParser.parse(document);
            final String imageUrl = uploadOrgImage(parsed.imageUrl());
            return new StoreDetailSnapshot(
                    snapshot.storeId(), snapshot.storeName(), snapshot.address(), snapshot.latitude(),
                    snapshot.longitude(), snapshot.placeUrl(), imageUrl,
                    parsed.businessHours(), parsed.openStatus());
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
        final URI uri = URI.create(imageUrl);
        if (!ALLOWED_HOSTS.contains(uri.getHost())) {
            return null;
        }
        final Connection.Response imageResponse = Jsoup.connect(imageUrl)
                .ignoreContentType(true)
                .timeout(5000)
                .maxBodySize(MAX_IMAGE_BYTES)
                .execute();
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


    private void validateKakaoUrl(final String url) {
        final URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !ALLOWED_HOSTS.contains(uri.getHost())) {
            throw new IllegalArgumentException("Unsupported Kakao URL");
        }
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
