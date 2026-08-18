package com.example.demo.report.infrastructure;

import com.example.demo.report.application.command.StoreSnapshot;
import com.example.demo.report.application.port.StoreCommandPort;
import com.example.demo.report.domain.Store;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StoreCommandAdapter implements StoreCommandPort {

    private static final String UPSERT_STORE = """
            INSERT INTO stores (
                kakao_place_id, place_name, place_url, category_name, address_name,
                road_address_name, phone, category_group_code, category_group_name,
                longitude, latitude, distance
            ) VALUES (
                :kakaoPlaceId, :placeName, :placeUrl, :categoryName, :addressName,
                :roadAddressName, :phone, :categoryGroupCode, :categoryGroupName,
                :longitude, :latitude, :distance
            )
            ON CONFLICT (kakao_place_id) DO UPDATE SET
                place_name = EXCLUDED.place_name,
                place_url = EXCLUDED.place_url,
                category_name = EXCLUDED.category_name,
                address_name = EXCLUDED.address_name,
                road_address_name = EXCLUDED.road_address_name,
                phone = EXCLUDED.phone,
                category_group_code = EXCLUDED.category_group_code,
                category_group_name = EXCLUDED.category_group_name,
                longitude = EXCLUDED.longitude,
                latitude = EXCLUDED.latitude,
                distance = EXCLUDED.distance,
                updated_at = CURRENT_TIMESTAMP
            RETURNING store_id
            """;

    private final EntityManager entityManager;

    @Override
    public boolean exists(final Long storeId) {
        return entityManager.find(Store.class, storeId) != null;
    }

    @Override
    public Long save(final StoreSnapshot snapshot) {
        final var query = entityManager.createNativeQuery(UPSERT_STORE)
                .setParameter("kakaoPlaceId", snapshot.kakaoPlaceId())
                .setParameter("placeName", snapshot.placeName())
                .setParameter("placeUrl", snapshot.placeUrl())
                .setParameter("categoryName", snapshot.categoryName())
                .setParameter("addressName", snapshot.addressName())
                .setParameter("roadAddressName", snapshot.roadAddressName())
                .setParameter("phone", snapshot.phone())
                .setParameter("categoryGroupCode", snapshot.categoryGroupCode())
                .setParameter("categoryGroupName", snapshot.categoryGroupName())
                .setParameter("longitude", snapshot.longitude())
                .setParameter("latitude", snapshot.latitude())
                .setParameter("distance", snapshot.distance());
        return ((Number) query.getSingleResult()).longValue();
    }
}
