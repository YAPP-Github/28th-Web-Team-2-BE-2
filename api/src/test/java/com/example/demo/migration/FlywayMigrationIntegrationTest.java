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

        assertThat(migrationVersions()).containsExactly(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21");
        assertThat(countRows("regions")).isEqualTo(467);
        assertThat(regionName("1121510100")).isEqualTo("서울특별시 광진구 중곡동");
        assertThat(countRows("public_prices")).isEqualTo(3);
        assertThat(countRows("batch_job_execution")).isZero();
        assertThat(countRows("batch_item_errors")).isZero();
        assertThat(countRows("news_articles")).isZero();
        assertThat(countRows("item_favorites")).isZero();
        assertThat(countRows("stores")).isZero();
        assertThat(countRows("user_reports")).isZero();
        assertThat(countRows("store_favorites")).isZero();
        assertThat(columnNames("item_favorites"))
                .containsExactly("item_favorite_id", "user_id", "item_id", "created_at");
        assertThat(constraintNames("item_favorites"))
                .contains("uk_item_favorites_user_item", "fk_item_favorites_user", "fk_item_favorites_item");
        assertThat(columnNames("users")).contains("nickname");
        assertThat(constraintNames("users")).contains("uk_users_nickname");
        assertThat(columnNames("items")).contains("category_code");
        assertThat(countRowsWhere("category_code = 'ROOT_VEGETABLES'")).isEqualTo(5);
        assertThat(countRowsWhere("category_code = 'LEAFY_GREENS'")).isEqualTo(12);
        assertThat(countRowsWhere("category_code = 'FRUITING_VEGETABLES'")).isEqualTo(8);
        assertThat(countRowsWhere("category_code = 'PEPPERS'")).isEqualTo(4);
        assertThat(countRowsWhere("category_code = 'SEASONINGS'")).isEqualTo(10);
        assertThat(countRowsWhere("category_code = 'MUSHROOMS'")).isEqualTo(3);
        assertThat(countRowsWhere("category_code = 'FRUITS'")).isEqualTo(4);
        assertThat(countRowsWhere("category_code IS NULL")).isZero();
        assertCategoryMapping();
        assertThat(columnNames("stores"))
                .containsExactly(
                        "store_id", "kakao_place_id", "place_name", "place_url", "category_name",
                        "address_name", "road_address_name", "phone", "category_group_code",
                        "category_group_name", "longitude", "latitude", "distance", "created_at", "updated_at",
                        "image_url", "business_hours", "open_status", "kakao_details_collected_at");
        assertThat(constraintNames("stores"))
                .contains("stores_pkey", "uk_stores_kakao_place_id");
        assertThat(columnNames("user_reports"))
                .containsExactly(
                        "report_id", "store_id", "item_id", "user_id", "price", "unit", "amount",
                        "report_date", "public_price_diff", "price_diff_rate", "photo_url", "created_at",
                        "region_id", "report_type");
        assertThat(constraintNames("user_reports"))
                .contains(
                        "user_reports_pkey", "fk_user_reports_store", "fk_user_reports_item",
                        "fk_user_reports_user", "ck_user_reports_price_positive", "ck_user_reports_amount_positive",
                        "ck_user_reports_report_type");
        assertThat(indexNames("user_reports"))
                .contains(
                        "idx_user_reports_item_report_date",
                        "idx_user_reports_user_created_at",
                        "uk_user_reports_submission");
        assertThat(columnNumericPrecision("user_reports", "price_diff_rate")).isEqualTo(14);
        assertThat(columnNames("store_favorites"))
                .containsExactly("store_favorite_id", "user_id", "store_id", "created_at");
        assertThat(constraintNames("store_favorites"))
                .contains("uk_store_favorites_user_store", "fk_store_favorites_user", "fk_store_favorites_store");
        assertThat(columnNames("user_regions"))
                .containsExactly("user_region_id", "user_id", "region_id", "is_current", "created_at");
        assertThat(constraintNames("user_regions"))
                .contains("uk_user_regions_user_region", "fk_user_regions_user", "fk_user_regions_region");
        assertThat(foreignKeyTargets("user_regions"))
                .contains("fk_user_regions_region=regions.region_id");
        assertThat(indexNames("user_regions"))
                .contains("idx_user_regions_user_current", "uk_user_regions_current_user");
    }

    @Test
    void 기존_V1부터_V8까지의_이력에서_V9부터_V14가_추가된다() throws SQLException {
        flyway().clean();
        flyway("8").migrate();

        final int itemsBefore = countRows("items");
        final int usersBefore = countRows("users");
        final int onlinePricesBefore = countRows("online_prices");

        flyway().migrate();

        assertThat(migrationVersions()).containsExactly(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21");
        assertThat(countRows("items")).isEqualTo(itemsBefore);
        assertThat(countRows("users")).isEqualTo(usersBefore);
        assertThat(countRows("regions")).isEqualTo(467);
        assertThat(countRows("public_prices")).isEqualTo(3);
        assertThat(countRows("online_prices")).isEqualTo(onlinePricesBefore);
        assertThat(countRows("batch_job_execution")).isZero();
        assertThat(countRows("batch_item_errors")).isZero();
        assertThat(countRows("news_articles")).isZero();
        assertThat(countRows("item_favorites")).isZero();
        assertThat(countRows("stores")).isZero();
        assertThat(countRows("user_reports")).isZero();
        assertThat(countRows("store_favorites")).isZero();
        assertThat(countRowsWhere("category_code IS NULL")).isZero();
    }

    @Test
    void 기존_V11_이력에_V12부터_V14_migration을_추가한다() throws SQLException {
        flyway().clean();
        flyway("11").migrate();

        assertThat(columnNames("items")).doesNotContain("category_code");

        flyway().migrate();

        assertThat(migrationVersions()).containsExactly(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21");
        assertCategoryMapping();
        assertThat(countRows("stores")).isZero();
        assertThat(countRows("user_reports")).isZero();
        assertThat(countRows("store_favorites")).isZero();
    }

    @Test
    void 기존_V13_이력에_V14_store_favorites를_추가한다() throws SQLException {
        flyway().clean();
        flyway("13").migrate();

        assertThat(columnNames("store_favorites")).isEmpty();

        flyway().migrate();

        assertThat(migrationVersions()).containsExactly(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21");
        assertThat(columnNames("store_favorites"))
                .containsExactly("store_favorite_id", "user_id", "store_id", "created_at");
        assertThat(constraintNames("store_favorites"))
                .contains("uk_store_favorites_user_store", "fk_store_favorites_user", "fk_store_favorites_store");
    }

    @Test
    void 기존_V14_제보_이력을_보존하면서_V15_지역과_제보_유형을_추가한다() throws SQLException {
        flyway().clean();
        flyway("14").migrate();
        executeUpdate("""
                INSERT INTO users (provider, provider_subject, name, role)
                VALUES ('KAKAO', 'migration-user', 'migration user', 'USER')
                """);
        executeUpdate("""
                INSERT INTO stores (kakao_place_id, place_name, address_name)
                VALUES ('migration-store', 'migration store', 'migration address')
                """);
        executeUpdate("""
                INSERT INTO user_reports (store_id, item_id, user_id, price, unit, amount, report_date)
                SELECT s.store_id, 1, u.id, 1000, 'kg', 1, CURRENT_DATE
                  FROM stores s
                  JOIN users u ON u.provider_subject = 'migration-user'
                 WHERE s.kakao_place_id = 'migration-store'
                """);

        flyway().migrate();

        assertThat(migrationVersions()).containsExactly(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21");
        assertThat(columnNullable("user_reports", "store_id")).isTrue();
        assertThat(columnNullable("user_reports", "user_id")).isTrue();
        assertThat(columnNullable("user_reports", "region_id")).isTrue();
        assertThat(columnNullable("user_reports", "report_type")).isTrue();
        assertThat(reportRegionAndType()).containsExactly(null, null);

        executeUpdate("DELETE FROM users WHERE provider_subject = 'migration-user'");
        assertThat(countRows("user_reports")).isEqualTo(1);
        assertThat(reportUserId()).isNull();
    }

    @Test
    void nickname은_null을_여러_개_허용하고_중복을_거부한다() throws SQLException {
        flyway().clean();
        flyway().migrate();

        executeUpdate("""
                INSERT INTO users (provider, provider_subject, email, name, role, nickname)
                VALUES ('KAKAO', 'nickname-1', NULL, '사용자 1', 'USER', '같은이름')
                """);
        executeUpdate("""
                INSERT INTO users (provider, provider_subject, email, name, role, nickname)
                VALUES ('KAKAO', 'nickname-2', NULL, '사용자 2', 'USER', NULL)
                """);
        executeUpdate("""
                INSERT INTO users (provider, provider_subject, email, name, role, nickname)
                VALUES ('KAKAO', 'nickname-3', NULL, '사용자 3', 'USER', NULL)
                """);

        assertThatThrownBy(() -> executeUpdate("""
                INSERT INTO users (provider, provider_subject, email, name, role, nickname)
                VALUES ('KAKAO', 'nickname-4', NULL, '사용자 4', 'USER', '같은이름')
                """))
                .isInstanceOf(SQLException.class);
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

    private int countRowsWhere(final String predicate) throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT COUNT(*) FROM items WHERE " + predicate)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void assertCategoryMapping() throws SQLException {
        assertThat(categoryCodes()).containsExactly(
                "ROOT_VEGETABLES",
                "SEASONINGS",
                "SEASONINGS",
                "ROOT_VEGETABLES",
                "ROOT_VEGETABLES",
                "FRUITING_VEGETABLES",
                "FRUITING_VEGETABLES",
                "LEAFY_GREENS",
                "ROOT_VEGETABLES",
                "FRUITING_VEGETABLES",
                "FRUITING_VEGETABLES",
                "FRUITING_VEGETABLES",
                "FRUITING_VEGETABLES",
                "FRUITING_VEGETABLES",
                "SEASONINGS",
                "SEASONINGS",
                "PEPPERS",
                "PEPPERS",
                "PEPPERS",
                "PEPPERS",
                "MUSHROOMS",
                "MUSHROOMS",
                "MUSHROOMS",
                "SEASONINGS",
                "SEASONINGS",
                "LEAFY_GREENS",
                "LEAFY_GREENS",
                "LEAFY_GREENS",
                "LEAFY_GREENS",
                "SEASONINGS",
                "SEASONINGS",
                "ROOT_VEGETABLES",
                "SEASONINGS",
                "SEASONINGS",
                "LEAFY_GREENS",
                "LEAFY_GREENS",
                "FRUITING_VEGETABLES",
                "FRUITS",
                "LEAFY_GREENS",
                "LEAFY_GREENS",
                "FRUITS",
                "FRUITS",
                "FRUITS",
                "LEAFY_GREENS",
                "LEAFY_GREENS",
                "LEAFY_GREENS");
    }

    private List<String> categoryCodes() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT category_code FROM items ORDER BY item_id")) {
            final ArrayList<String> categories = new ArrayList<>();
            while (resultSet.next()) {
                categories.add(resultSet.getString("category_code"));
            }
            return categories;
        }
    }

    private int executeUpdate(final String sql) throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    private boolean columnNullable(final String tableName, final String columnName) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.prepareStatement(
                        "SELECT is_nullable FROM information_schema.columns "
                                + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return "YES".equals(resultSet.getString("is_nullable"));
            }
        }
    }

    private int columnNumericPrecision(final String tableName, final String columnName) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.prepareStatement(
                        "SELECT numeric_precision FROM information_schema.columns "
                                + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private List<String> reportRegionAndType() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT region_id, report_type FROM user_reports ORDER BY report_id")) {
            final ArrayList<String> values = new ArrayList<>();
            while (resultSet.next()) {
                values.add(resultSet.getString("region_id"));
                values.add(resultSet.getString("report_type"));
            }
            return values;
        }
    }

    private Long reportUserId() throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT user_id FROM user_reports ORDER BY report_id LIMIT 1")) {
            resultSet.next();
            return (Long) resultSet.getObject(1);
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

    private List<String> foreignKeyTargets(final String tableName) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.prepareStatement(
                        "SELECT tc.constraint_name || '=' || ccu.table_name || '.' || ccu.column_name "
                                + "FROM information_schema.table_constraints tc "
                                + "JOIN information_schema.constraint_column_usage ccu "
                                + "ON ccu.constraint_schema = tc.constraint_schema "
                                + "AND ccu.constraint_name = tc.constraint_name "
                                + "WHERE tc.constraint_schema = 'public' AND tc.table_name = ? "
                                + "AND tc.constraint_type = 'FOREIGN KEY'")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                final ArrayList<String> targets = new ArrayList<>();
                while (resultSet.next()) {
                    targets.add(resultSet.getString(1));
                }
                return targets;
            }
        }
    }

    private String regionName(final String regionId) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.prepareStatement(
                        "SELECT region_name FROM regions WHERE region_id = ?")) {
            statement.setString(1, regionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString(1);
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
