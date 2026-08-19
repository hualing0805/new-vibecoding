package com.threadmind.forum.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FlywayMySqlMigrationIT {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/threadmind_migration_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";
    private static final String DEFAULT_USERNAME = "threadmind_test";
    private static final String DEFAULT_PASSWORD = "threadmind_test";

    @Test
    void migratesV1ToV4OnEmptyMysqlSchema() throws SQLException {
        MigrationTarget target = MigrationTarget.fromEnvironment();
        String database = databaseName(target.url());
        assertTrue(
                database.toLowerCase(Locale.ROOT).contains("test"),
                "Refusing to clean a non-test database: " + database
        );

        assertCanConnect(target);

        Flyway flyway = Flyway.configure()
                .dataSource(target.url(), target.username(), target.password())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        MigrateResult result = flyway.migrate();

        assertEquals(4, result.migrationsExecuted, "Expected V1-V4 to run on an empty schema.");

        try (Connection connection = DriverManager.getConnection(target.url(), target.username(), target.password())) {
            assertEquals(4, queryLong(
                    connection,
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version IN ('1', '2', '3', '4')"
            ));
            assertTableExists(connection, "users");
            assertTableExists(connection, "roles");
            assertTableExists(connection, "topics");
            assertTableExists(connection, "notifications");
            assertTableExists(connection, "ai_sessions");
            assertTableExists(connection, "topic_ai_summaries");
            assertEquals(1, queryLong(
                    connection,
                    "SELECT COUNT(*) FROM users WHERE username = ? AND password_hash = ?",
                    "demo",
                    "{noop}ThreadMind@123"
            ));
        }
    }

    private static void assertCanConnect(MigrationTarget target) {
        try (Connection ignored = DriverManager.getConnection(target.url(), target.username(), target.password())) {
            // Connection check only.
        } catch (SQLException ex) {
            fail("Cannot connect to MySQL migration test database. Start MySQL and run scripts/mysql-test-bootstrap.sql before this test. Cause: "
                    + ex.getMessage(), ex);
        }
    }

    private static void assertTableExists(Connection connection, String tableName) throws SQLException {
        long count = queryLong(
                connection,
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                tableName
        );
        assertEquals(1, count, "Expected table to exist: " + tableName);
    }

    private static long queryLong(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i += 1) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "Expected one row for query: " + sql);
                return resultSet.getLong(1);
            }
        }
    }

    private static String databaseName(String jdbcUrl) {
        String withoutParameters = jdbcUrl.split("\\?", 2)[0];
        int slash = withoutParameters.lastIndexOf('/');
        if (slash < 0 || slash == withoutParameters.length() - 1) {
            throw new IllegalArgumentException("JDBC URL must include a database name.");
        }
        return withoutParameters.substring(slash + 1);
    }

    private static String setting(String propertyName, String environmentName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return defaultValue;
    }

    private record MigrationTarget(String url, String username, String password) {
        static MigrationTarget fromEnvironment() {
            return new MigrationTarget(
                    setting("threadmind.it.mysql.url", "THREADMIND_MYSQL_IT_URL", DEFAULT_URL),
                    setting("threadmind.it.mysql.username", "THREADMIND_MYSQL_IT_USERNAME", DEFAULT_USERNAME),
                    setting("threadmind.it.mysql.password", "THREADMIND_MYSQL_IT_PASSWORD", DEFAULT_PASSWORD)
            );
        }
    }
}
