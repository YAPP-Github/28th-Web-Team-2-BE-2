package com.example.demo.kamis.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.kamis.application.port.KamisPriceQueryPort;
import com.example.demo.kamis.application.query.KamisDailyPriceQuery;
import com.example.demo.kamis.application.result.KamisDailyPriceItemResult;
import com.example.demo.kamis.application.result.KamisDailyPriceResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@Import(CollectKamisPublicPriceIntegrationTest.FixtureConfiguration.class)
class CollectKamisPublicPriceIntegrationTest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193";
    private static final String REGION_ID = "1144010200";
    private static final LocalDate PRICE_DATE = LocalDate.of(2026, 8, 20);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("app")
            .withUsername("app")
            .withPassword("test-password");

    @DynamicPropertySource
    static void configurePostgres(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private CollectKamisPublicPriceUseCase useCase;

    @Autowired
    private ItemJpaRepository itemJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Item item;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM public_prices WHERE region_id = ?", REGION_ID);
        item = itemJpaRepository.saveAndFlush(
                new Item("KAMIS 통합 저장 테스트", "1kg", null, ItemCategory.ROOT_VEGETABLES));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM public_prices WHERE item_id = ?", item.id());
        itemJpaRepository.deleteById(item.id());
    }

    @Test
    void KAMIS_조회부터_public_prices_저장까지_실제_PostgreSQL에서_수행한다() {
        final int saved = useCase.execute(REGION_ID, "1101", PRICE_DATE);

        assertThat(saved).isEqualTo(1);
        final Map<String, Object> price = jdbcTemplate.queryForMap(
                "SELECT price, region_id, price_date FROM public_prices WHERE item_id = ?",
                item.id());
        assertThat(price)
                .containsEntry("price", 37_300)
                .containsEntry("region_id", REGION_ID)
                .containsEntry("price_date", java.sql.Date.valueOf(PRICE_DATE));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixtureConfiguration {

        @Bean(name = "fixtureKamisPriceQueryPort")
        @Primary
        KamisPriceQueryPort kamisPriceQueryPort() {
            return this::findDailyPrices;
        }

        private KamisDailyPriceResult findDailyPrices(final KamisDailyPriceQuery query) {
            if (!"100".equals(query.itemCategoryCode())) {
                return new KamisDailyPriceResult("000", null, List.of());
            }
            return new KamisDailyPriceResult(
                    "000",
                    null,
                    List.of(new KamisDailyPriceItemResult(
                            "KAMIS 통합 저장 테스트", null, null, null, "상품", "20kg", "당일", "37,300",
                            null, null, null, null, null, null, null, null, null, null, null, null)));
        }
    }
}
