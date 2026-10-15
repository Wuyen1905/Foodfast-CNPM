package com.foodfast.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in integration test for the real MySQL dialect.
 *
 * It cleans only the dedicated foodfast_test database supplied by the caller.
 * Normal `mvn test` remains fast and isolated on H2.
 */
@EnabledIfSystemProperty(named = "foodfast.mysql.test", matches = "true")
class MySqlMigrationIntegrationTest {

    @Test
    void mysqlMigrationsBuildAUsableDevelopmentSchema() throws Exception {
        String url = requiredProperty("foodfast.mysql.url");
        String username = requiredProperty("foodfast.mysql.username");
        String password = requiredProperty("foodfast.mysql.password");

        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/mysql", "classpath:db/mysql-dev")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        var migration = flyway.migrate();
        flyway.validate();

        assertEquals("14", migration.targetSchemaVersion.toString());
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            assertEquals(7, scalar(statement, "SELECT COUNT(*) FROM users"));
            assertEquals(2, scalar(statement, "SELECT COUNT(*) FROM users WHERE role = 'manager'"));
            assertEquals(2, scalar(statement, "SELECT COUNT(*) FROM users WHERE role = 'manager' AND password LIKE '$2%'"));
            assertEquals(2, scalar(statement, """
                    SELECT COUNT(*) FROM users
                    WHERE username IN ('quanly', 'quanly1')
                      AND manager_permissions = 'ORDER_MANAGEMENT,CUSTOMER_SUPPORT,PAYMENT_RECONCILIATION,RESTAURANT_SETTLEMENT'
                    """));
            try (ResultSet managers = statement.executeQuery("SELECT password FROM users WHERE role = 'manager'")) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                int verified = 0;
                while (managers.next()) {
                    assertTrue(passwordEncoder.matches("quanly123", managers.getString(1)));
                    verified++;
                }
                assertEquals(2, verified);
            }
            assertEquals(8, scalar(statement, "SELECT COUNT(*) FROM products"));
            assertEquals(7, scalar(statement, "SELECT COUNT(*) FROM restaurant_couriers"));
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'order_groups'"));
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'order_group_id'"));
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'restaurant_settlements'"));
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'restaurant_settlement_orders'"));
            assertTrue(scalar(statement, "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1") >= 14);
        }
    }

    private static long scalar(Statement statement, String sql) throws Exception {
        try (ResultSet row = statement.executeQuery(sql)) {
            row.next();
            return row.getLong(1);
        }
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + name);
        }
        return value;
    }
}
