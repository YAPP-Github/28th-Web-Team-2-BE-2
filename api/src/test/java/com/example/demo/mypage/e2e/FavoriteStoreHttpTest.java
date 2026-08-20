package com.example.demo.mypage.e2e;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.report.domain.Store;
import com.example.demo.store.domain.StoreFavorite;
import com.example.demo.store.infrastructure.persistence.StoreFavoriteJpaRepository;
import com.example.demo.store.infrastructure.persistence.StoreJpaRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FavoriteStoreHttpTest {

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final StoreJpaRepository storeJpaRepository;
    private final StoreFavoriteJpaRepository storeFavoriteJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    FavoriteStoreHttpTest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final StoreJpaRepository storeJpaRepository,
            final StoreFavoriteJpaRepository storeFavoriteJpaRepository,
            final JwtTokenProvider jwtTokenProvider) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.storeJpaRepository = storeJpaRepository;
        this.storeFavoriteJpaRepository = storeFavoriteJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Test
    void 현재_사용자의_단골만_좌표_기준_거리순으로_페이지응답한다() throws Exception {
        final User currentUser = saveUser("현재 사용자");
        final User otherUser = saveUser("다른 사용자");
        final Store nearbyStore = saveStore("가까운 가게", "37.501", "127.000");
        final Store distantStore = saveStore("먼 가게", "37.600", "127.000");
        final Store otherUserStore = saveStore("다른 사용자 가게", "37.502", "127.000");
        favorite(currentUser, nearbyStore);
        favorite(currentUser, distantStore);
        favorite(otherUser, otherUserStore);

        mockMvc.perform(get("/api/v1/users/me/favorite-stores")
                        .queryParam("latitude", "37.500")
                        .queryParam("longitude", "127.000")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(currentUser))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.stores[0].storeId").value(nearbyStore.id().intValue()))
                .andExpect(jsonPath("$.data.stores[0].storeName").value("가까운 가게"))
                .andExpect(jsonPath("$.data.stores[0].distanceMeters").isNumber())
                .andExpect(jsonPath("$.data.stores[0].storeImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.stores[0].openStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.stores[0].todayBusinessHours").value(nullValue()))
                .andExpect(jsonPath("$.data.stores[0].isLiked").value(true));

        mockMvc.perform(get("/api/v1/users/me/favorite-stores")
                        .queryParam("latitude", "37.500")
                        .queryParam("longitude", "127.000")
                        .queryParam("page", "1")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(currentUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stores[0].storeId").value(distantStore.id().intValue()))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void 좌표가_없으면_storeId_오름차순과_null_거리를_반환한다() throws Exception {
        final User user = saveUser("좌표 없는 사용자");
        final Store firstStore = saveStore("첫 가게", "37.500", "127.000");
        final Store secondStore = saveStore("둘째 가게", "37.600", "127.000");
        favorite(user, firstStore);
        favorite(user, secondStore);

        mockMvc.perform(get("/api/v1/users/me/favorite-stores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.stores[0].storeId").value(firstStore.id().intValue()))
                .andExpect(jsonPath("$.data.stores[0].distanceMeters").value(nullValue()))
                .andExpect(jsonPath("$.data.stores[1].storeId").value(secondStore.id().intValue()))
                .andExpect(jsonPath("$.data.stores[1].distanceMeters").value(nullValue()));
    }

    @Test
    void 단골이_없으면_빈_페이지를_반환한다() throws Exception {
        final User user = saveUser("빈 목록 사용자");

        mockMvc.perform(get("/api/v1/users/me/favorite-stores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stores").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void 페이지_입력이_잘못되면_공통_400을_반환한다() throws Exception {
        final User user = saveUser("잘못된 페이지 사용자");

        mockMvc.perform(get("/api/v1/users/me/favorite-stores")
                        .queryParam("page", "-1")
                        .queryParam("size", "0")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void 좌표가_일부만_입력되면_공통_400을_반환한다() throws Exception {
        final User user = saveUser("불완전한 좌표 사용자");
        final String token = bearer(accessToken(user));

        mockMvc.perform(get("/api/v1/users/me/favorite-stores")
                        .queryParam("latitude", "37.5")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));

        mockMvc.perform(get("/api/v1/users/me/favorite-stores")
                        .queryParam("longitude", "127.0")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void 비로그인과_GUEST는_단골_가게를_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/favorite-stores"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me/favorite-stores")
                        .with(SecurityMockMvcRequestPostProcessors.user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 단골_가게_조회_계약을_OpenAPI에_노출한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/favorite-stores'].get.responses['200']")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.FavoriteStoresResponse.properties.stores")
                        .exists());
    }

    private User saveUser(final String name) {
        return userJpaRepository.saveAndFlush(User.oauth(
                ProviderType.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                name));
    }

    private Store saveStore(final String name, final String latitude, final String longitude) {
        return storeJpaRepository.saveAndFlush(new Store(
                "kakao-" + UUID.randomUUID().toString().substring(0, 8),
                name,
                null,
                null,
                "주소",
                null,
                null,
                null,
                null,
                new BigDecimal(longitude),
                new BigDecimal(latitude),
                null));
    }

    private void favorite(final User user, final Store store) {
        storeFavoriteJpaRepository.saveAndFlush(new StoreFavorite(user.id(), store.id()));
    }

    private String accessToken(final User user) {
        return jwtTokenProvider.createAccessToken(user.id(), user.role());
    }

    private String bearer(final String token) {
        return "Bearer " + token;
    }
}
