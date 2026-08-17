package com.example.demo.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class FlywayMigrationIntegrationTest {

    private static final String POSTGRES_IMAGE =
            "postgres:17-alpine@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("app")
            .withUsername("app")
            .withPassword("test-password");

    @Test
    void 빈_PostgreSQL에_모든_migration이_순서대로_적용된다() throws SQLException {
        final Flyway flyway = flyway();

        flyway.clean();
        flyway.migrate();

        assertThat(migrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
        assertThat(countRows("regions")).isEqualTo(467);
        assertThat(countRows("public_prices")).isEqualTo(3);
        assertThat(countRows("batch_job_execution")).isZero();
        assertThat(countRows("batch_item_errors")).isZero();
        assertThat(countRows("news_articles")).isZero();
        assertThat(countRows("item_favorites")).isZero();
        assertThat(countRows("stores")).isZero();
        assertThat(countRows("user_reports")).isZero();
        assertThat(columnNames("item_favorites"))
                .containsExactly("item_favorite_id", "user_id", "item_id", "created_at");
        assertThat(constraintNames("item_favorites"))
                .contains("uk_item_favorites_user_item", "fk_item_favorites_user", "fk_item_favorites_item");
        assertThat(columnNames("stores"))
                .containsExactly(
                        "store_id", "kakao_place_id", "place_name", "place_url", "category_name",
                        "address_name", "road_address_name", "phone", "category_group_code",
                        "category_group_name", "longitude", "latitude", "distance", "created_at", "updated_at");
        assertThat(constraintNames("stores"))
                .contains("stores_pkey", "uk_stores_kakao_place_id");
        assertThat(columnNames("user_reports"))
                .containsExactly(
                        "report_id", "store_id", "item_id", "user_id", "price", "unit", "amount",
                        "report_date", "public_price_diff", "price_diff_rate", "photo_url", "created_at");
        assertThat(constraintNames("user_reports"))
                .contains(
                        "user_reports_pkey", "fk_user_reports_store", "fk_user_reports_item",
                        "fk_user_reports_user", "ck_user_reports_price_positive", "ck_user_reports_amount_positive");
        assertThat(indexNames("user_reports"))
                .contains("idx_user_reports_item_report_date", "idx_user_reports_user_created_at");
    }

    @Test
    void 기존_V1부터_V8까지의_이력에서_V9와_V10이_추가된다() throws SQLException {
        flyway().clean();
        flyway("8").migrate();

        final int itemsBefore = countRows("items");
        final int usersBefore = countRows("users");
        final int onlinePricesBefore = countRows("online_prices");

        flyway().migrate();

        assertThat(migrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
        assertThat(countRows("items")).isEqualTo(itemsBefore);
        assertThat(countRows("users")).isEqualTo(usersBefore);
        assertThat(countRows("regions")).isEqualTo(467);
        assertThat(countRows("public_prices")).isEqualTo(3);
        assertThat(countRows("online_prices")).isEqualTo(onlinePricesBefore);
        assertThat(countRows("batch_job_execution")).isZero();
        assertThat(countRows("batch_item_errors")).isZero();
        assertThat(countRows("news_articles")).isZero();
        assertThat(countRows("item_favorites")).isZero();
    }

    @Test
    void 품목과_채널이_삭제되어도_batch_item_error_이력은_보존한다() throws SQLException {
        flyway().clean();
        flyway().migrate();
        executeUpdate("""
                INSERT INTO batch_job_execution (
                    job_name, status, started_at, ended_at, total_records, success_records
                ) VALUES ('ONLINE_PRICE_COLLECTION', 'FAILED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0)
                """);
        executeUpdate("""
                INSERT INTO batch_item_errors (
                    job_execution_id, item_id, channel_id, attempt_count,
                    error_type, error_message, created_at
                ) VALUES (1, 1, 1, 1, 'EXTERNAL_API_ERROR', '정제된 오류', CURRENT_TIMESTAMP)
                """);

        final int deletedItems = executeUpdate("DELETE FROM items WHERE item_id = 1");
        final int deletedChannels = executeUpdate(
                "DELETE FROM online_channels WHERE channel_id = 1");

        assertThat(deletedItems).isEqualTo(1);
        assertThat(deletedChannels).isEqualTo(1);
        assertThat(countRows("batch_item_errors")).isEqualTo(1);
    }

    @Test
    void 같은_품목_채널_날짜_URL의_온라인_가격은_중복_저장할_수_없다() throws SQLException {
        flyway().clean();
        flyway().migrate();
        final String insert = onlinePriceInsert("https://example.com/product");

        executeUpdate(insert);

        assertThatThrownBy(() -> executeUpdate(insert)).isInstanceOf(SQLException.class);
    }

    @Test
    void V7에_중복_온라인_가격이_있으면_삭제하지_않고_V8을_거부한다() throws SQLException {
        flyway().clean();
        flyway("7").migrate();
        final String insert = onlinePriceInsert("https://example.com/duplicate");
        executeUpdate(insert);
        executeUpdate(insert);

        assertThatThrownBy(() -> flyway().migrate())
                .rootCause()
                .hasMessageContaining("V8 requires unique online price scope and product URL");
        assertThat(countRows("online_prices")).isEqualTo(2);
    }

    private Flyway flyway() {
        return flyway(null);
    }

    private Flyway flyway(final String target) {
        final FluentConfiguration configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private List<String> migrationVersions() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT version FROM flyway_schema_history ORDER BY installed_rank")) {
            final ArrayList<String> versions = new ArrayList<>();
            while (resultSet.next()) {
                versions.add(resultSet.getString("version"));
            }
            return versions;
        }
    }

    private int countRows(final String tableName) throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int executeUpdate(final String sql) throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    private String onlinePriceInsert(final String productUrl) {
        return """
                INSERT INTO online_prices (
                    item_id, channel_id, item_name, product_name, price, unit, product_url, created_at
                ) VALUES (
                    1, 1, '감자', '감자 상품', 1000, 100, '%s', CURRENT_DATE
                )
                """.formatted(productUrl);
    }

    private List<String> columnNames(final String tableName) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.prepareStatement(
                        "SELECT column_name FROM information_schema.columns "
                                + "WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                final ArrayList<String> columns = new ArrayList<>();
                while (resultSet.next()) {
                    columns.add(resultSet.getString("column_name"));
                }
                return columns;
            }
        }
    }

    private List<String> constraintNames(final String tableName) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.prepareStatement(
                        "SELECT constraint_name FROM information_schema.table_constraints "
                                + "WHERE table_schema = 'public' AND table_name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                final ArrayList<String> constraints = new ArrayList<>();
                while (resultSet.next()) {
                    constraints.add(resultSet.getString("constraint_name"));
                }
                return constraints;
            }
        }
    }

    private List<String> indexNames(final String tableName) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.prepareStatement(
                        "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                final ArrayList<String> indexes = new ArrayList<>();
                while (resultSet.next()) {
                    indexes.add(resultSet.getString("indexname"));
                }
                return indexes;
            }
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
