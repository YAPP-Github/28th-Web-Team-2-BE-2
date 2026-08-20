package com.example.demo.kamis.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.item.infrastructure.ItemJpaRepository;
import com.example.demo.kamis.application.port.PublicPriceCommandPort.PublicPriceCommand;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class PublicPriceCommandAdapterIntegrationTest {

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
    private PublicPriceCommandAdapter adapter;

    @Autowired
    private ItemJpaRepository itemJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long itemId;

    @BeforeEach
    void setUp() {
        itemId = itemJpaRepository.save(
                new Item("KAMIS 저장 테스트", "1kg", null, ItemCategory.ROOT_VEGETABLES)).id();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM public_prices WHERE item_id = ?", itemId);
        itemJpaRepository.deleteById(itemId);
    }

    @Test
    void 같은_품목_지역_날짜를_재수집하면_행을_중복하지_않고_가격을_갱신한다() {
        final PublicPriceCommand first = new PublicPriceCommand(itemId, REGION_ID, 1_200, PRICE_DATE);
        final PublicPriceCommand second = new PublicPriceCommand(itemId, REGION_ID, 1_300, PRICE_DATE);

        adapter.upsertAll(List.of(first));
        adapter.upsertAll(List.of(second));

        final List<Integer> prices = jdbcTemplate.queryForList(
                "SELECT price FROM public_prices WHERE item_id = ? AND region_id = ? AND price_date = ?",
                Integer.class, itemId, REGION_ID, PRICE_DATE);
        assertThat(prices).containsExactly(1_300);
    }
}
