package com.foodfast.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThatCode;

class DatabaseMigrationSmokeTest {
    @Test
    void operationalTimestampMigrationIsApplied() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:foodfast-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration", "classpath:db/dev")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertThatCode(() -> jdbcTemplate.queryForList(
                "SELECT preparing_at, ready_at, delivering_at, delivered_at FROM orders"))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbcTemplate.queryForList(
                "SELECT customer_id, customer_name FROM support_tickets"))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbcTemplate.queryForList(
                "SELECT ticket_id, sender_role, message FROM support_ticket_messages"))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbcTemplate.queryForList(
                "SELECT order_id, from_status, to_status, actor_role FROM order_status_history"))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbcTemplate.queryForList(
                "SELECT user_id, title, is_read FROM customer_notifications"))
                .doesNotThrowAnyException();
    }
}
