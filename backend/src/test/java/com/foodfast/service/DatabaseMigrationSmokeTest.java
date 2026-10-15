package com.foodfast.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThatCode(() -> jdbcTemplate.queryForList(
                "SELECT status, payable_amount, created_by, paid_by FROM restaurant_settlements"))
                .doesNotThrowAnyException();
        assertThatCode(() -> jdbcTemplate.queryForList(
                "SELECT settlement_id, order_id, payable_amount FROM restaurant_settlement_orders"))
                .doesNotThrowAnyException();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM restaurant_couriers", Long.class)).isEqualTo(7L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM users
                WHERE username IN ('quanly', 'quanly1')
                  AND manager_permissions = 'ORDER_MANAGEMENT,CUSTOMER_SUPPORT,PAYMENT_RECONCILIATION,RESTAURANT_SETTLEMENT'
                """, Long.class)).isEqualTo(2L);

        jdbcTemplate.update("""
                INSERT INTO complaints (
                  id, order_id, restaurant_id, customer_id, category, description,
                  status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, "COMPLAINT-SMOKE", "ORDER-001", "rest_2", "u2", "OTHER",
                "Báo cáo sự cố không yêu cầu hoàn tiền", "OPEN", 1690000000000L, 1690000000000L);
        jdbcTemplate.update("""
                INSERT INTO complaint_history (
                  complaint_id, action, actor_role, actor_id, to_status, note, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "COMPLAINT-SMOKE", "CREATED", "CUSTOMER", "u2", "OPEN",
                "Tạo báo cáo sự cố", 1690000000000L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT requested_refund_amount FROM complaints WHERE id = ?",
                Integer.class, "COMPLAINT-SMOKE")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT refund_amount FROM complaint_history WHERE complaint_id = ?",
                Integer.class, "COMPLAINT-SMOKE")).isZero();

        jdbcTemplate.update("""
                INSERT INTO restaurant_settlements (
                  id, restaurant_id, restaurant_name, period_start, period_end, status,
                  order_count, food_amount, restaurant_tax_amount, restaurant_discount_amount,
                  platform_discount_amount, commission_rate_bps, commission_amount, refund_amount,
                  adjustment_amount, payable_amount, created_by, created_at, version
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP, DATEADD('DAY', 1, CURRENT_TIMESTAMP), ?,
                  1, 105000, 0, 0, 0, 1500, 15750, 0, 0, 89250, ?, CURRENT_TIMESTAMP, 0)
                """, "SETTLEMENT-TEST", "rest_2", "Tiệm bánh SweetDreams", "PENDING_REVIEW", "manager_2");
        jdbcTemplate.update("""
                INSERT INTO restaurant_settlement_orders (
                  settlement_id, order_id, food_amount, restaurant_tax_amount,
                  restaurant_discount_amount, platform_discount_amount, commission_amount,
                  refund_amount, payable_amount
                ) VALUES (?, ?, 105000, 0, 0, 0, 15750, 0, 89250)
                """, "SETTLEMENT-TEST", "ORDER-001");
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO restaurant_settlement_orders (
                  settlement_id, order_id, food_amount, restaurant_tax_amount,
                  restaurant_discount_amount, platform_discount_amount, commission_amount,
                  refund_amount, payable_amount
                ) VALUES (?, ?, 105000, 0, 0, 0, 15750, 0, 89250)
                """, "SETTLEMENT-TEST", "ORDER-001"))
                .hasMessageContaining("Unique");
    }
}
