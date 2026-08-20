package com.example.demo.store.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.UserRole;
import static org.assertj.core.api.Assertions.assertThat;
import com.example.demo.common.exception.GlobalExceptionHandler;
import com.example.demo.common.presentation.ResponseWrapper;
import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.port.RecommendedStoreQueryPort;
import com.example.demo.store.application.port.StorePersistencePort;
import com.example.demo.store.application.query.StoreDetailQuery;
import com.example.demo.store.application.result.StoreDetailResult;
import com.example.demo.store.application.usecase.GetNearbyStoresUseCase;
import com.example.demo.store.application.usecase.GetRecommendedStoresUseCase;
import com.example.demo.store.application.usecase.GetStoreDetailUseCase;
import com.example.demo.store.application.usecase.StoreFavoriteUseCase;
import com.example.demo.store.presentation.converter.StoreCommandConverter;
import com.example.demo.store.presentation.dto.StoreDetailRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.example.demo.store.presentation.converter.StoreQueryConverter;
import com.example.demo.store.presentation.converter.StoreResultConverter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    GlobalExceptionHandler.class,
    ResponseWrapper.class,
    StoreCommandConverter.class,
    StoreQueryConverter.class,
    StoreResultConverter.class,
    StoreDetailControllerTest.MockBeans.class
})
class StoreDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GetStoreDetailUseCase getStoreDetailUseCase;

    @Test
    void 공개_가게_상세는_공통_응답과_계약된_null_필드를_반환한다() throws Exception {
        Mockito.when(getStoreDetailUseCase.execute(Mockito.any(StoreDetailQuery.class)))
                .thenReturn(result(false, null));

        mockMvc.perform(get("/api/v1/stores/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.storeId").value(1))
                .andExpect(jsonPath("$.data.storeName").value("장보고 마트"))
                .andExpect(jsonPath("$.data.storeImageUrl").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.favoriteCount").value(0))
                .andExpect(jsonPath("$.data.cheapItemCount").value(0))
                .andExpect(jsonPath("$.data.expensiveItemCount").value(0))
                .andExpect(jsonPath("$.data.totalReportedItemCount").value(0))
                .andExpect(jsonPath("$.data.regionId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.regionName").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.latestReportedDate").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.latestReportedAt").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.address").value("서울 강남구 삼성동 123"))
                .andExpect(jsonPath("$.data.latitude").value(37.5088))
                .andExpect(jsonPath("$.data.longitude").value(127.0632))
                .andExpect(jsonPath("$.data.distance").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.walkTimeMinutes").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.businessHours").isArray())
                .andExpect(jsonPath("$.data.businessHours").isEmpty())
                .andExpect(jsonPath("$.data.openStatus").value("UNKNOWN"));
    }

    @Test
    void 좌표가_제공되면_상세_응답의_거리만_채우고_도보시간은_null이다() throws Exception {
        Mockito.when(getStoreDetailUseCase.execute(Mockito.any(StoreDetailQuery.class)))
                .thenReturn(result(false, 340));

        mockMvc.perform(get("/api/v1/stores/1")
                        .queryParam("latitude", "37.50")
                        .queryParam("longitude", "127.06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.distance").value(340))
                .andExpect(jsonPath("$.data.walkTimeMinutes").value(Matchers.nullValue()));
    }

    @Test
    void 좌표는_둘_다_생략하거나_둘_다_제공해야_한다() throws Exception {
        Mockito.when(getStoreDetailUseCase.execute(Mockito.any(StoreDetailQuery.class)))
                .thenReturn(result(false, null));

        mockMvc.perform(get("/api/v1/stores/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/stores/1").queryParam("latitude", "37.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void storeId와_좌표_범위가_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));

        mockMvc.perform(get("/api/v1/stores/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));

        mockMvc.perform(get("/api/v1/stores/1")
                        .queryParam("latitude", "90.1")
                        .queryParam("longitude", "127"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));

        mockMvc.perform(get("/api/v1/stores/1")
                        .queryParam("latitude", "37")
                        .queryParam("longitude", "180.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void 없는_가게는_404를_반환한다() throws Exception {
        Mockito.when(getStoreDetailUseCase.execute(Mockito.any(StoreDetailQuery.class)))
                .thenThrow(new com.example.demo.common.exception.ApiException(
                        "not found",
                        com.example.demo.common.exception.ErrorType.NO_RESOURCE_ERROR,
                        org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/stores/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));
    }

    @Test
    void ROLE_USER는_본인_ID를_상세_조회_쿼리로_전달하고_GUEST는_익명으로_취급한다() {
        final StoreDetailRequest request = new StoreDetailRequest(null, null);
        final Authentication userAuthentication = authenticationObject(7L, UserRole.USER.authority());
        final Authentication guestAuthentication = authenticationObject(7L, "ROLE_GUEST");

        assertThat(new StoreQueryConverter()
                .toStoreDetailQuery(1L, request, new AuthPrincipal(7L), userAuthentication)
                .userId()).isEqualTo(7L);
        assertThat(new StoreQueryConverter()
                .toStoreDetailQuery(1L, request, new AuthPrincipal(7L), guestAuthentication)
                .userId()).isNull();
    }

    private StoreDetailResult result(final boolean liked, final Integer distance) {
        return new StoreDetailResult(
                1L,
                "장보고 마트",
                null,
                liked,
                0,
                0,
                0,
                0,
                null,
                null,
                null,
                null,
                "서울 강남구 삼성동 123",
                new BigDecimal("37.5088"),
                new BigDecimal("127.0632"),
                distance,
                null,
                List.of(),
                "UNKNOWN");
    }

    private Authentication authenticationObject(final Long userId, final String authority) {
        return new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(userId), null, List.of(new SimpleGrantedAuthority(authority)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockBeans {

        @Bean
        NearbyStoreSearchPort nearbyStoreSearchPort() {
            return Mockito.mock(NearbyStoreSearchPort.class);
        }

        @Bean
        StorePersistencePort storePersistencePort() {
            return Mockito.mock(StorePersistencePort.class);
        }

        @Bean
        GetNearbyStoresUseCase getNearbyStoresUseCase(
                final NearbyStoreSearchPort searchPort,
                final StorePersistencePort persistencePort) {
            return new GetNearbyStoresUseCase(searchPort, persistencePort);
        }

        @Bean
        GetStoreDetailUseCase getStoreDetailUseCase() {
            return Mockito.mock(GetStoreDetailUseCase.class);
        }

        @Bean
        RecommendedStoreQueryPort recommendedStoreQueryPort() {
            return Mockito.mock(RecommendedStoreQueryPort.class);
        }

        @Bean
        GetRecommendedStoresUseCase getRecommendedStoresUseCase(
                final RecommendedStoreQueryPort queryPort) {
            return new GetRecommendedStoresUseCase(queryPort);
        }

        @Bean
        StoreFavoriteUseCase storeFavoriteUseCase() {
            return Mockito.mock(StoreFavoriteUseCase.class);
        }
    }
}
