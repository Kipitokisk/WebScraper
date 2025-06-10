package scraper.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import scraper.model.CarDetails;
import scraper.model.LookupEntity;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class LookupEntityMapperIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withInitScript("init.sql");

    private DatabaseManager dbManager;
    private LookupEntityMapper lookupEntityMapper;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        dbManager = new DatabaseManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        lookupEntityMapper = new LookupEntityMapper("ad_type", dbManager);
        connection = dbManager.getConnection();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET CONSTRAINTS ALL DEFERRED");
                stmt.execute("TRUNCATE TABLE cars, particularities, wheel_side, nr_of_seats, body, " +
                        "nr_of_doors, engine_capacity, horsepower, petrol_type, gears_type, " +
                        "traction_type, color, ad_type RESTART IDENTITY CASCADE");
                stmt.execute("SET CONSTRAINTS ALL IMMEDIATE");
            }
            connection.close();
        }
    }

    @Test
    void testGetOrInsertLookup_InsertNewValue() throws SQLException {
        Integer id = lookupEntityMapper.getOrInsertLookup("Vând");
        assertNotNull(id, "ID should be returned for new value");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM ad_type WHERE name = 'Vând'")) {
            assertTrue(rs.next(), "Value should be inserted");
            assertEquals(id.intValue(), rs.getInt("id"), "ID should match");
            assertEquals("Vând", rs.getString("name"), "Name should match");
        }
    }

    @Test
    void testGetOrInsertLookup_ExistingValue() throws SQLException {
        lookupEntityMapper.getOrInsertLookup("Vând");
        Integer id = lookupEntityMapper.getOrInsertLookup("Vând");
        assertNotNull(id, "ID should be returned for existing value");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM ad_type WHERE name = 'Vând'")) {
            assertTrue(rs.next(), "Should have a result");
            assertEquals(1, rs.getInt("count"), "Only one record should exist");
        }
    }

    @Test
    void testGetOrInsertLookup_NullValue() throws SQLException {
        Integer id = lookupEntityMapper.getOrInsertLookup(null);
        assertNull(id, "Null value should return null ID");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM ad_type")) {
            assertTrue(rs.next(), "Should have a result");
            assertEquals(0, rs.getInt("count"), "No records should be inserted");
        }
    }

    @Test
    void testMap_CarDetails() {
        CarDetails carDetails = new CarDetails.Builder().adType("Vând").build();
        LookupEntity entity = lookupEntityMapper.map(carDetails);
        assertNotNull(entity, "Mapped entity should not be null");
        assertNull(entity.getName(), "Name should be null as per map implementation");
        assertNull(entity.getId(), "ID should be null initially");
    }

    @Test
    void testSave_ValidCarDetails() throws SQLException {
        CarDetails carDetails = new CarDetails.Builder().adType("Vând").build();
        lookupEntityMapper.save(carDetails);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM ad_type WHERE name = 'Vând'")) {
            assertTrue(rs.next(), "Value should be inserted");
            assertEquals("Vând", rs.getString("name"), "Name should match");
            assertTrue(rs.getInt("id") > 0, "ID should be positive");
        }
    }

    @Test
    void testSave_NullAdType() throws SQLException {
        CarDetails carDetails = new CarDetails.Builder().build(); // No adType set
        lookupEntityMapper.save(carDetails);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM ad_type")) {
            assertTrue(rs.next(), "Should have a result");
            assertEquals(0, rs.getInt("count"), "No records should be inserted");
        }
    }

    @Test
    void testSaveBatch_MultipleCarDetails() throws SQLException {
        CarDetails car1 = new CarDetails.Builder().adType("Vând").build();
        CarDetails car2 = new CarDetails.Builder().adType("Cumpăr").build();
        CarDetails car3 = new CarDetails.Builder().adType("Vând").build();
        lookupEntityMapper.saveBatch(Arrays.asList(car1, car2, car3));

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, COUNT(*) AS count FROM ad_type GROUP BY name")) {
            int vandCount = 0, cumparCount = 0;
            while (rs.next()) {
                if ("Vând".equals(rs.getString("name"))) vandCount = rs.getInt("count");
                if ("Cumpăr".equals(rs.getString("name"))) cumparCount = rs.getInt("count");
            }
            assertEquals(1, vandCount, "Vând should appear once due to ON CONFLICT");
            assertEquals(1, cumparCount, "Cumpăr should appear once");
        }
    }

    @Test
    void testSaveBatch_EmptyList() throws SQLException {
        lookupEntityMapper.saveBatch(Collections.emptyList());

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM ad_type")) {
            assertTrue(rs.next(), "Should have a result");
            assertEquals(0, rs.getInt("count"), "No records should be inserted");
        }
    }
}