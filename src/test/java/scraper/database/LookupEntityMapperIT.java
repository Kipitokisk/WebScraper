package scraper.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class LookupEntityMapperIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("scraper_db")
            .withUsername("postgres")
            .withPassword("pass")
            .withInitScript("init.sql");

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
        clearDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private void clearDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE wheel_side RESTART IDENTITY CASCADE");
        }
    }

    @Test
    void testGetOrInsertLookup_NullValue_ReturnsNull() throws SQLException {
        LookupEntityMapper lookupEntityMapper = new LookupEntityMapper("wheel_side", null);
        Integer result = lookupEntityMapper.getOrInsertLookup(connection);
        assertNull(result);
    }

    @Test
    void testGetOrInsertLookup_InsertsAndReturnsId() throws SQLException {
        String value = "TestName";
        LookupEntityMapper lookupEntityMapper = new LookupEntityMapper("wheel_side", value);
        Integer id = lookupEntityMapper.getOrInsertLookup(connection);
        assertNotNull(id);
        assertTrue(id > 0);

        try (PreparedStatement stmt = connection.prepareStatement("SELECT id, name FROM wheel_side WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(value, rs.getString("name"));
            }
        }
    }

    @Test
    void testGetOrInsertLookup_ReturnsExistingId() throws SQLException {
        String value = "ExistingName";
        Integer firstId;
        try (PreparedStatement insertStmt = connection.prepareStatement(
                "INSERT INTO wheel_side (\"name\") VALUES (?) RETURNING id")) {
            insertStmt.setString(1, value);
            try (ResultSet rs = insertStmt.executeQuery()) {
                assertTrue(rs.next());
                firstId = rs.getInt("id");
            }
        }

        LookupEntityMapper lookupEntityMapper = new LookupEntityMapper("wheel_side", value);
        Integer secondId = lookupEntityMapper.getOrInsertLookup(connection);
        assertNotNull(secondId);
        assertEquals(firstId, secondId);
    }
}