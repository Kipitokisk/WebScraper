package scraper.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DatabaseManagerIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    private DatabaseManager dbManager;
    private Connection connection;

    @BeforeEach
    void setUp() {
        dbManager = new DatabaseManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        try {
            connection = dbManager.getConnection();
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS test_table (id SERIAL PRIMARY KEY, name VARCHAR(255))");
            }
        } catch (SQLException e) {
            fail("Failed to set up test database: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void testGetConnection_ValidCredentials() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            assertTrue(rs.next(), "Query should return a result");
            assertEquals(1, rs.getInt(1), "Query should return value 1");
            assertFalse(conn.isClosed(), "Connection should be open");
        }
    }

    @Test
    void testGetConnection_IndependentConnections() throws SQLException {
        try (Connection conn1 = dbManager.getConnection();
             Connection conn2 = dbManager.getConnection()) {
            assertNotSame(conn1, conn2, "Each getConnection call should return a new connection");
            assertFalse(conn1.isClosed(), "First connection should be open");
            assertFalse(conn2.isClosed(), "Second connection should be open");

            conn1.close();
            assertTrue(conn1.isClosed(), "First connection should be closed");
            assertFalse(conn2.isClosed(), "Second connection should remain open");
        }
    }

    @Test
    void testGetConnection_InvalidCredentials() {
        DatabaseManager invalidDbManager = new DatabaseManager(
                postgres.getJdbcUrl(),
                "wrong_user",
                "wrong_pass"
        );
        SQLException thrown = assertThrows(SQLException.class, invalidDbManager::getConnection,
                "Should throw SQLException for invalid credentials");
        assertTrue(thrown.getMessage().contains("authentication failed") || thrown.getMessage().contains("password authentication failed"),
                "Exception message should indicate authentication failure");
    }

    @Test
    void testPrepareStatement_ValidSql() throws SQLException {
        String sql = "INSERT INTO test_table (name) VALUES (?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, sql)) {
            assertNotNull(stmt, "PreparedStatement should not be null");
            stmt.setString(1, "TestName");
            int rowsAffected = stmt.executeUpdate();
            assertEquals(1, rowsAffected, "One row should be inserted");

            try (Statement verifyStmt = conn.createStatement();
                 ResultSet rs = verifyStmt.executeQuery("SELECT name FROM test_table WHERE name = 'TestName'")) {
                assertTrue(rs.next(), "Inserted row should exist");
                assertEquals("TestName", rs.getString("name"), "Name should match inserted value");
            }
        }
    }

    @Test
    void testPrepareStatement_InvalidSql() throws SQLException {
        String invalidSql = "INVALID SQL SYNTAX";
        try (Connection conn = dbManager.getConnection()) {
            SQLException thrown = assertThrows(SQLException.class,
                    () -> {
                        try (PreparedStatement stmt = dbManager.prepareStatement(conn, invalidSql)) {
                            stmt.execute();
                        }
                    },
                    "Should throw SQLException for invalid SQL");
            assertTrue(thrown.getMessage().toLowerCase().contains("syntax error"),
                    "Exception message should indicate syntax error");
        }
    }

}