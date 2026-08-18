package com.example.demo.store.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.common.exception.GlobalExceptionHandler;
import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.port.StorePersistencePort;
import com.example.demo.store.application.query.NearbyStoreQuery;
import com.example.demo.store.application.result.NearbyStoreCandidate;
import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoreSearchResult;
import com.example.demo.store.application.result.NearbyStoresResult;
import com.example.demo.store.application.usecase.GetNearbyStoresUseCase;
import com.example.demo.store.presentation.converter.StoreQueryConverter;
import com.example.demo.store.presentation.converter.StoreResultConverter;
import com.example.demo.store.presentation.dto.NearbyStoreRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    StoreQueryConverter.class,
    StoreResultConverter.class,
    GlobalExceptionHandler.class,
    StoreControllerTest.MockBeans.class
})
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NearbyStoreSearchPort nearbyStoreSearchPort;

    @Autowired
    private StorePersistencePort storePersistencePort;

    @BeforeEach
    void setUp() {
        final NearbyStoreCandidate candidate = new NearbyStoreCandidate(
                "kakao-1",
                "장보고 마트",
                new BigDecimal("37.5088"),
                new BigDecimal("127.0632"),
                "서울 강남구 삼성동 123",
                "서울 강남구 테헤란로 123",
                "02-1234-5678",
                "http://place.map.kakao.com/1",
                670);
        final NearbyStoreResult store = new NearbyStoreResult(
                1L,
                "장보고 마트",
                new BigDecimal("37.5088"),
                new BigDecimal("127.0632"),
                "서울 강남구 삼성동 123",
                "서울 강남구 테헤란로 123",
                "02-1234-5678",
                "http://place.map.kakao.com/1",
                670,
                false);
        when(nearbyStoreSearchPort.search(any(NearbyStoreQuery.class)))
                .thenReturn(new NearbyStoreSearchResult(List.of(candidate)));
        when(storePersistencePort.synchronize(any()))
                .thenReturn(List.of(store));
    }

    @Test
    void 공개_조회는_직접_응답과_숫자형_storeId를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "37.5088")
                        .queryParam("longitude", "127.0632"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.stores[0].storeId").value(1))
                .andExpect(jsonPath("$.stores[0].isLiked").value(false));
    }

    @Test
    void 결과가_없으면_200과_빈_목록을_반환한다() throws Exception {
        when(nearbyStoreSearchPort.search(any(NearbyStoreQuery.class)))
                .thenReturn(new NearbyStoreSearchResult(List.of()));
        when(storePersistencePort.synchronize(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "37.5")
                        .queryParam("longitude", "127.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.stores").isEmpty());
    }

    @Test
    void keyword는_앞뒤_공백을_제거해_조회_쿼리로_전달한다() throws Exception {
        clearInvocations(nearbyStoreSearchPort);
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "37.5")
                        .queryParam("longitude", "127.0")
                        .queryParam("keyword", "  장보고 마트  "))
                .andExpect(status().isOk());

        final ArgumentCaptor<NearbyStoreQuery> queryCaptor =
                ArgumentCaptor.forClass(NearbyStoreQuery.class);
        verify(nearbyStoreSearchPort).search(queryCaptor.capture());
        assertThat(queryCaptor.getValue().keyword()).isEqualTo("장보고 마트");
    }

    @Test
    void 좌표와_반경이_잘못되면_공통_400_오류를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "91")
                        .queryParam("longitude", "127")
                        .queryParam("radius", "5001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void 위도와_경도가_누락되면_공통_400_오류를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "37.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void onlyLiked가_잘못된_boolean이면_공통_400_오류를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "37")
                        .queryParam("longitude", "127")
                        .queryParam("onlyLiked", "not-boolean"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));
    }

    @Test
    void onlyLiked는_비로그인_사용자에게_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/stores/nearby")
                        .queryParam("latitude", "37")
                        .queryParam("longitude", "127")
                        .queryParam("onlyLiked", "true"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void ROLE_USER_인증_주체를_현재_사용자_쿼리로_변환한다() {
        final NearbyStoreRequest request = new NearbyStoreRequest(
                new BigDecimal("37"),
                new BigDecimal("127"),
                2000,
                false,
                null);
        final var authentication = new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(7L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        final NearbyStoreQuery query = new StoreQueryConverter()
                .toNearbyStoreQuery(request, new AuthPrincipal(7L), authentication);

        assertThat(query.roleUser()).isTrue();
        assertThat(query.userId()).isEqualTo(7L);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockBeans {

        @Bean
        NearbyStoreSearchPort nearbyStoreSearchPort() {
            return mock(NearbyStoreSearchPort.class);
        }

        @Bean
        StorePersistencePort storePersistencePort() {
            return mock(StorePersistencePort.class);
        }

        @Bean
        GetNearbyStoresUseCase getNearbyStoresUseCase(
                final NearbyStoreSearchPort searchPort,
                final StorePersistencePort persistencePort) {
            return new GetNearbyStoresUseCase(searchPort, persistencePort);
        }
    }
}
