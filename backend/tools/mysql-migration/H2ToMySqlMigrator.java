import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One-time local migration utility used when moving FoodFast from H2 to MySQL.
 *
 * It deliberately preserves Flyway's MySQL history, replaces all application
 * table contents, keeps generated identifiers, and verifies every row count.
 */
public final class H2ToMySqlMigrator {
    private static final String H2_URL =
            "jdbc:h2:file:./runtime-data/foodfast;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;" +
            "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_ON_EXIT=FALSE";

    private H2ToMySqlMigrator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: H2ToMySqlMigrator <mysql-url> <mysql-user> <mysql-password>");
        }

        Class.forName("org.h2.Driver");
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection source = DriverManager.getConnection(H2_URL, "sa", "");
             Connection target = DriverManager.getConnection(args[0], args[1], args[2])) {
            source.setReadOnly(true);
            target.setAutoCommit(false);

            List<String> sourceTables = sourceTables(source);
            Set<String> targetTables = targetTables(target);
            List<String> copyTables = sourceTables.stream()
                    .filter(targetTables::contains)
                    .filter(table -> !table.equals("flyway_schema_history"))
                    .toList();

            if (copyTables.isEmpty()) {
                throw new IllegalStateException("No matching FoodFast tables were found");
            }

            try (Statement statement = target.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                for (String table : copyTables) {
                    statement.executeUpdate("DELETE FROM " + quote(table));
                }
            }

            try {
                for (String table : copyTables) {
                    copyTable(source, target, table);
                }
                target.commit();
            } catch (Exception exception) {
                target.rollback();
                throw exception;
            } finally {
                try (Statement statement = target.createStatement()) {
                    statement.execute("SET FOREIGN_KEY_CHECKS=1");
                }
            }

            verifyCounts(source, target, copyTables);
            System.out.println("Migration completed: " + copyTables.size() + " tables verified.");
        }
    }

    private static List<String> sourceTables(Connection source) throws Exception {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement statement = source.prepareStatement(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                "ORDER BY table_name");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                tables.add(rows.getString(1).toLowerCase(Locale.ROOT));
            }
        }
        return tables;
    }

    private static Set<String> targetTables(Connection target) throws Exception {
        Set<String> tables = new HashSet<>();
        DatabaseMetaData metadata = target.getMetaData();
        try (ResultSet rows = metadata.getTables(target.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rows.next()) {
                tables.add(rows.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return tables;
    }

    private static void copyTable(Connection source, Connection target, String table) throws Exception {
        String selectSql = "SELECT * FROM " + quote(table);
        try (Statement select = source.createStatement();
             ResultSet rows = select.executeQuery(selectSql)) {
            ResultSetMetaData metadata = rows.getMetaData();
            int columnCount = metadata.getColumnCount();
            List<String> columns = new ArrayList<>(columnCount);
            for (int index = 1; index <= columnCount; index++) {
                columns.add(metadata.getColumnName(index).toLowerCase(Locale.ROOT));
            }

            String placeholders = String.join(",", java.util.Collections.nCopies(columnCount, "?"));
            String insertSql = "INSERT INTO " + quote(table) + " (" +
                    columns.stream().map(H2ToMySqlMigrator::quote).reduce((a, b) -> a + "," + b).orElseThrow() +
                    ") VALUES (" + placeholders + ")";

            int copied = 0;
            try (PreparedStatement insert = target.prepareStatement(insertSql)) {
                while (rows.next()) {
                    for (int index = 1; index <= columnCount; index++) {
                        setValue(insert, index, rows.getObject(index));
                    }
                    insert.addBatch();
                    copied++;
                    if (copied % 200 == 0) {
                        insert.executeBatch();
                    }
                }
                insert.executeBatch();
            }
            System.out.printf("%-32s %d rows%n", table, copied);
        }
    }

    private static void setValue(PreparedStatement statement, int index, Object value) throws Exception {
        if (value instanceof Clob clob) {
            statement.setString(index, clob.getSubString(1, Math.toIntExact(clob.length())));
        } else if (value instanceof Blob blob) {
            statement.setBytes(index, blob.getBytes(1, Math.toIntExact(blob.length())));
        } else if (value instanceof OffsetDateTime dateTime) {
            statement.setTimestamp(index, Timestamp.from(dateTime.toInstant()));
        } else if (value instanceof ZonedDateTime dateTime) {
            statement.setTimestamp(index, Timestamp.from(dateTime.toInstant()));
        } else if (value instanceof Instant instant) {
            statement.setTimestamp(index, Timestamp.from(instant));
        } else if (value instanceof LocalDateTime dateTime) {
            statement.setTimestamp(index, Timestamp.valueOf(dateTime));
        } else {
            statement.setObject(index, value);
        }
    }

    private static void verifyCounts(Connection source, Connection target, List<String> tables) throws Exception {
        for (String table : tables) {
            long sourceCount = count(source, table);
            long targetCount = count(target, table);
            if (sourceCount != targetCount) {
                throw new IllegalStateException(
                        "Row count mismatch for " + table + ": H2=" + sourceCount + ", MySQL=" + targetCount);
            }
        }
    }

    private static long count(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM " + quote(table))) {
            row.next();
            return row.getLong(1);
        }
    }

    private static String quote(String identifier) {
        if (!identifier.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Unsafe SQL identifier: " + identifier);
        }
        return "`" + identifier + "`";
    }
}
