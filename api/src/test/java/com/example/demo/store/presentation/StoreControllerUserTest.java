package com.example.demo.store.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.common.exception.GlobalExceptionHandler;
import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.store.application.port.NearbyStoreSearchPort;
import com.example.demo.store.application.port.StorePersistencePort;
import com.example.demo.store.application.result.NearbyStoreCandidate;
import com.example.demo.store.application.result.NearbyStoreResult;
import com.example.demo.store.application.result.NearbyStoreSearchResult;
import com.example.demo.store.application.usecase.GetNearbyStoresUseCase;
import com.example.demo.store.presentation.converter.StoreQueryConverter;
import com.example.demo.store.presentation.converter.StoreResultConverter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StoreControllerUserTest {

    private final NearbyStoreSearchPort nearbyStoreSearchPort = mock(NearbyStoreSearchPort.class);
    private final StorePersistencePort storePersistencePort = mock(StorePersistencePort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StoreController(
                        new GetNearbyStoresUseCase(nearbyStoreSearchPort, storePersistencePort),
                        new StoreQueryConverter(),
                        new StoreResultConverter()))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        final NearbyStoreCandidate candidate = new NearbyStoreCandidate(
                "kakao-1",
                "장보고 마트",
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                "주소",
                "도로명",
                "전화",
                "url",
                100);
        final NearbyStoreResult store = new NearbyStoreResult(
                1L,
                "장보고 마트",
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                "주소",
                "도로명",
                "전화",
                "url",
                100,
                false);
        when(nearbyStoreSearchPort.search(any()))
                .thenReturn(new NearbyStoreSearchResult(List.of(candidate)));
        when(storePersistencePort.synchronize(any())).thenReturn(List.of(store));
        when(storePersistencePort.findLikedStoreIds(7L, List.of(1L))).thenReturn(Set.of(1L));
    }

    @Test
    void ROLE_USER의_현재_단골이_HTTP_isLiked_true로_응답된다() throws Exception {
        final Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(7L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        final var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        try {
            mockMvc.perform(get("/api/v1/stores/nearby")
                            .principal(authentication)
                            .queryParam("latitude", "37.5")
                            .queryParam("longitude", "127.0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stores[0].storeId").value(1))
                    .andExpect(jsonPath("$.stores[0].isLiked").value(true));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void ROLE_USER의_onlyLiked_조회는_200과_단골_목록을_반환한다() throws Exception {
        final Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(7L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        final var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        try {
            mockMvc.perform(get("/api/v1/stores/nearby")
                            .principal(authentication)
                            .queryParam("latitude", "37.5")
                            .queryParam("longitude", "127.0")
                            .queryParam("onlyLiked", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.stores[0].storeId").value(1))
                    .andExpect(jsonPath("$.stores[0].isLiked").value(true));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
