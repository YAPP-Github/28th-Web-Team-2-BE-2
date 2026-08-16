package com.example.demo.migration;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(migrationVersions()).containsExactly("1", "2", "3", "4", "5");
        assertThat(countRows("public_prices")).isEqualTo(3);
    }

    @Test
    void 기존_V1부터_V4까지의_이력에서_V5만_추가된다() throws SQLException {
        flyway().clean();
        flyway("4").migrate();

        final int itemsBefore = countRows("items");
        final int usersBefore = countRows("users");

        flyway().migrate();

        assertThat(migrationVersions()).containsExactly("1", "2", "3", "4", "5");
        assertThat(countRows("items")).isEqualTo(itemsBefore);
        assertThat(countRows("users")).isEqualTo(usersBefore);
        assertThat(countRows("public_prices")).isEqualTo(3);
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

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
