package com.example.demo.report.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.domain.ProviderType;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.infrastructure.persistence.UserJpaRepository;
import com.example.demo.auth.infrastructure.token.JwtTokenProvider;
import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.domain.PublicPrice;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.item.infrastructure.PublicPriceJpaRepository;
import com.example.demo.report.domain.ReportType;
import com.example.demo.report.domain.UserReport;
import com.example.demo.report.infrastructure.UserReportJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MyReportCommandE2ETest {

    private static final String PATH = "/api/v1/users/me/reports";
    private static final String REGION_ID = "1121510100";

    private final MockMvc mockMvc;
    private final UserJpaRepository userJpaRepository;
    private final ItemJpaRepository itemJpaRepository;
    private final PublicPriceJpaRepository publicPriceJpaRepository;
    private final UserReportJpaRepository userReportJpaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private Long itemId;

    @Autowired
    MyReportCommandE2ETest(
            final MockMvc mockMvc,
            final UserJpaRepository userJpaRepository,
            final ItemJpaRepository itemJpaRepository,
            final PublicPriceJpaRepository publicPriceJpaRepository,
            final UserReportJpaRepository userReportJpaRepository,
            final JwtTokenProvider jwtTokenProvider) {
        this.mockMvc = mockMvc;
        this.userJpaRepository = userJpaRepository;
        this.itemJpaRepository = itemJpaRepository;
        this.publicPriceJpaRepository = publicPriceJpaRepository;
        this.userReportJpaRepository = userReportJpaRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @BeforeEach
    void setUp() {
        userReportJpaRepository.deleteAll();
        publicPriceJpaRepository.deleteAll();
        itemJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        final Item item = itemJpaRepository.save(
                new Item("감자", "1kg", null, ItemCategory.ROOT_VEGETABLES));
        itemId = item.id();
        publicPriceJpaRepository.save(new PublicPrice(
                itemId, REGION_ID, 3000, LocalDate.now(ZoneId.of("Asia/Seoul"))));
    }

    @Test
    @DisplayName("내 제보를 기존 reportId로 수정하고 가격 차이 스냅샷을 갱신한다")
    void updatesOwnedReportInPlace() throws Exception {
        final User me = saveUser("나");
        final UserReport report = saveReport(me.id(), 3500);
        final Long reportId = report.id();
        final LocalDate reportDate = report.reportDate();
        final var createdAt = report.createdAt();

        mockMvc.perform(patch(path(reportId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(me))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":3600,"unit":"1kg","amount":2.000}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        final UserReport updated = userReportJpaRepository.findById(reportId).orElseThrow();
        assertThat(updated.id()).isEqualTo(reportId);
        assertThat(updated.userId()).isEqualTo(me.id());
        assertThat(updated.regionId()).isEqualTo(REGION_ID);
        assertThat(updated.reportType()).isEqualTo(ReportType.PURCHASE);
        assertThat(updated.price()).isEqualTo(3600);
        assertThat(updated.unit()).isEqualTo("1kg");
        assertThat(updated.amount()).isEqualByComparingTo("2.000");
        assertThat(updated.publicPriceDiff()).isEqualTo(600);
        assertThat(updated.priceDiffRate()).isEqualByComparingTo("20.00");
        assertThat(updated.reportDate()).isEqualTo(reportDate);
        assertThat(updated.createdAt()).isEqualTo(createdAt.truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("다른 사용자의 제보는 수정할 수 없다")
    void rejectsUpdateOfAnotherUsersReport() throws Exception {
        final User owner = saveUser("작성자");
        final User other = saveUser("다른 사용자");
        final UserReport report = saveReport(owner.id(), 3500);

        mockMvc.perform(patch(path(report.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":3600,"unit":"1kg","amount":2.000}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));

        assertThat(userReportJpaRepository.findById(report.id()).orElseThrow().price())
                .isEqualTo(3500);
    }

    @Test
    @DisplayName("품목 기준 단위와 다른 단위는 제보를 수정하지 않는다")
    void rejectsUpdateWithMismatchedUnit() throws Exception {
        final User me = saveUser("나");
        final UserReport report = saveReport(me.id(), 3500);

        mockMvc.perform(patch(path(report.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(me))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":3600,"unit":"2kg","amount":2.000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_ERROR"));

        assertThat(userReportJpaRepository.findById(report.id()).orElseThrow().price())
                .isEqualTo(3500);
    }

    @Test
    @DisplayName("내 제보를 삭제하면 저장소에서 제거된다")
    void deletesOwnedReport() throws Exception {
        final User me = saveUser("나");
        final UserReport report = saveReport(me.id(), 3500);

        mockMvc.perform(delete(path(report.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(userReportJpaRepository.findById(report.id())).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 제보는 삭제할 수 없다")
    void rejectsDeleteOfAnotherUsersReport() throws Exception {
        final User owner = saveUser("작성자");
        final User other = saveUser("다른 사용자");
        final UserReport report = saveReport(owner.id(), 3500);

        mockMvc.perform(delete(path(report.id()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_RESOURCE_ERROR"));

        assertThat(userReportJpaRepository.findById(report.id())).isPresent();
    }

    @Test
    @DisplayName("제보 수정·삭제는 로그인하지 않으면 401이다")
    void rejectsUnauthenticatedCommands() throws Exception {
        mockMvc.perform(patch(path(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":3600,"unit":"1kg","amount":2.000}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(path(1L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("제보 수정·삭제 API가 OpenAPI 문서에 노출된다")
    void exposesApiDocs() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/reports/{reportId}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/reports/{reportId}'].delete").exists());
    }

    private String path(final Long reportId) {
        return PATH + "/" + reportId;
    }

    private UserReport saveReport(final Long userId, final int price) {
        return userReportJpaRepository.save(new UserReport(
                REGION_ID, ReportType.PURCHASE, null, itemId, userId, price, "1kg",
                new BigDecimal("1.000"), price - 3000, null, null));
    }

    private User saveUser(final String name) {
        return userJpaRepository.save(User.oauth(
                ProviderType.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@example.com",
                name));
    }

    private String bearer(final User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(user.id(), user.role());
    }
}
