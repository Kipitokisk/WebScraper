package scraper.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import scraper.model.CarDetails;

import java.sql.*;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DatabaseManagerIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("scraper_db")
            .withUsername("postgres")
            .withPassword("pass")
            .withInitScript("init.sql");

    private DatabaseManager databaseManager;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        databaseManager = new DatabaseManager(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
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
            stmt.execute("SET CONSTRAINTS ALL DEFERRED");
            stmt.execute("TRUNCATE TABLE cars, particularities, wheel_side, nr_of_seats, body, " +
                    "nr_of_doors, engine_capacity, horsepower, petrol_type, gears_type, " +
                    "traction_type, color, ad_type RESTART IDENTITY CASCADE");
            stmt.execute("SET CONSTRAINTS ALL IMMEDIATE");
        }
    }



    @Test
    void testSaveCars_ValidCar() throws SQLException {
        CarDetails car = new CarDetails.Builder().link("https://999.md/ro/car")
                .name("Renault Megane 2016")
                .eurPrice(15000)
                .mileage(100000)
                .updateDate(null)
                .adType("Vând")
                .region("Orhei")
                .author("John")
                .yearOfFabrication(2016)
                .wheelSide("Stânga")
                .nrOfSeats(5)
                .body("Sedan")
                .nrOfDoors(4)
                .engineCapacity(1800)
                .horsepower(132)
                .petrolType("Benzină")
                .gearsType("Automat")
                .tractionType("Față")
                .color("Alb").build();

        databaseManager.saveCars(Collections.singletonList(car));

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT c.link, c.region, c.mileage, c.price_eur, p.author, p.year_of_fabrication,
                            c.update_date, ad.name as ad_type,
                            ws.name as wheel_side, ns.name as nr_of_seats, b.name as body,
                            nd.name as nr_of_doors, ec.name as engine_capacity, hp.name as horsepower,
                            pt.name as petrol_type, gt.name as gears_type, tt.name as traction_type,
                            cl.name as color
                     FROM cars c
                     JOIN particularities p ON c.particularities_id = p.id
                     JOIN wheel_side ws ON p.wheel_side_id = ws.id
                     JOIN nr_of_seats ns ON p.nr_of_seats_id = ns.id
                     JOIN body b ON p.body_id = b.id
                     JOIN nr_of_doors nd ON p.nr_of_doors_id = nd.id
                     JOIN engine_capacity ec ON p.engine_capacity_id = ec.id
                     JOIN horsepower hp ON p.horsepower_id = hp.id
                     JOIN petrol_type pt ON p.petrol_type_id = pt.id
                     JOIN gears_type gt ON p.gears_type_id = gt.id
                     JOIN traction_type tt ON p.traction_type_id = tt.id
                     JOIN color cl ON p.color_id = cl.id
                     JOIN ad_type ad ON c.ad_type_id = ad.id
                     """)) {

            assertTrue(rs.next());
            assertEquals("https://999.md/ro/car", rs.getString("link"));
            assertEquals("Orhei", rs.getString("region"));
            assertEquals(100000, rs.getInt("mileage"));
            assertEquals(15000, rs.getInt("price_eur"));
            assertEquals("John", rs.getString("author"));
            assertEquals(2016, rs.getInt("year_of_fabrication"));
            assertEquals("Vând", rs.getString("ad_type"));
            assertEquals("Stânga", rs.getString("wheel_side"));
            assertEquals(5, rs.getInt("nr_of_seats"));
            assertEquals("Sedan", rs.getString("body"));
            assertEquals(4, rs.getInt("nr_of_doors"));
            assertEquals(1800, rs.getInt("engine_capacity"));
            assertEquals(132, rs.getInt("horsepower"));
            assertEquals("Benzină", rs.getString("petrol_type"));
            assertEquals("Automat", rs.getString("gears_type"));
            assertEquals("Față", rs.getString("traction_type"));
            assertEquals("Alb", rs.getString("color"));
        }
    }

    @Test
    void testSaveCarsWithNullValues() throws SQLException {
        CarDetails car = new CarDetails.Builder().link("https://999.md/ro/car")
                .name(null)
                .eurPrice(null)
                .mileage(null)
                .updateDate(null)
                .adType(null)
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();

        databaseManager.saveCars(Collections.singletonList(car));

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM cars JOIN particularities ON cars.particularities_id = particularities.id")) {
            assertTrue(rs.next());
            assertEquals("https://999.md/ro/car", rs.getString("link"));
            assertNull(rs.getString("region"));
            assertEquals(0, rs.getInt("mileage"));
            assertEquals(0, rs.getInt("price_eur"));
            assertNull(rs.getString("update_date"));
            assertNull(rs.getString("author"));
            assertEquals(0, rs.getInt("year_of_fabrication"));
            assertNull(rs.getObject("wheel_side_id"));
            assertNull(rs.getObject("nr_of_seats_id"));
            assertNull(rs.getObject("body_id"));
            assertNull(rs.getObject("nr_of_doors_id"));
            assertNull(rs.getObject("engine_capacity_id"));
            assertNull(rs.getObject("horsepower_id"));
            assertNull(rs.getObject("petrol_type_id"));
            assertNull(rs.getObject("gears_type_id"));
            assertNull(rs.getObject("traction_type_id"));
            assertNull(rs.getObject("color_id"));
            assertNull(rs.getObject("ad_type_id"));
        }
    }

    @Test
    void testSaveCars_EmptyList() throws SQLException {
        databaseManager.saveCars(Collections.emptyList());
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM cars")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("count"));
        }
    }

    @Test
    void testSetNullableString_WithNullValue() throws SQLException {
        String sql = "INSERT INTO wheel_side (name) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            assertThrows(SQLException.class, () -> {
                databaseManager.setNullableString(stmt, 1, null);
                stmt.executeUpdate();
            });
        }
    }

    @Test
    void testSetNullableString_WithValidValue() throws SQLException {
        String testValue = "Test Value";
        String sql = "INSERT INTO wheel_side (name) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            databaseManager.setNullableString(stmt, 1, testValue);
            stmt.executeUpdate();
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM wheel_side WHERE name = 'Test Value'")) {
            assertTrue(rs.next());
            assertEquals(testValue, rs.getString("name"));
        }
    }

    @Test
    void testSetNullableString_WithEmptyString() throws SQLException {
        String testValue = "";
        String sql = "INSERT INTO wheel_side (name) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            databaseManager.setNullableString(stmt, 1, testValue);
            stmt.executeUpdate();
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM wheel_side WHERE name = ''")) {
            assertTrue(rs.next());
            assertEquals(testValue, rs.getString("name"));
        }
    }

    @Test
    void testSetNullableInt_WithNullValue() throws SQLException {
        String sql = "INSERT INTO particularities (year_of_fabrication) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            databaseManager.setNullableInt(stmt, 1, null);
            stmt.executeUpdate();
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT year_of_fabrication FROM particularities WHERE year_of_fabrication IS NULL")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("year_of_fabrication")); // getInt returns 0 for NULL
            assertTrue(rs.wasNull());
        }
    }

    @Test
    void testSetNullableInt_WithValidValue() throws SQLException {
        Integer testValue = 2024;
        String sql = "INSERT INTO particularities (year_of_fabrication) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            databaseManager.setNullableInt(stmt, 1, testValue);
            stmt.executeUpdate();
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT year_of_fabrication FROM particularities WHERE year_of_fabrication = 2024")) {
            assertTrue(rs.next());
            assertEquals(testValue.intValue(), rs.getInt("year_of_fabrication"));
            assertFalse(rs.wasNull());
        }
    }

    @Test
    void testSetNullableInt_WithZeroValue() throws SQLException {
        Integer testValue = 0;
        String sql = "INSERT INTO particularities (year_of_fabrication) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            databaseManager.setNullableInt(stmt, 1, testValue);
            stmt.executeUpdate();
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT year_of_fabrication FROM particularities WHERE year_of_fabrication = 0")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("year_of_fabrication"));
            assertFalse(rs.wasNull());
        }
    }

    @Test
    void testGetOrInsertLookup_NullValue_ReturnsNull() throws SQLException {
        Integer result = databaseManager.getOrInsertLookup(connection, "wheel_side", null);
        assertNull(result);
    }

    @Test
    void testGetOrInsertLookup_InsertsAndReturnsId() throws SQLException {
        String value = "TestName";

        Integer id1 = databaseManager.getOrInsertLookup(connection, "wheel_side", value);
        assertNotNull(id1);
        assertTrue(id1 > 0);

        try (PreparedStatement stmt = connection.prepareStatement("SELECT id, name FROM wheel_side WHERE id = ?")) {
            stmt.setInt(1, id1);
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

        Integer secondId = databaseManager.getOrInsertLookup(connection, "wheel_side", value);
        assertNotNull(secondId);
        assertEquals(firstId, secondId);
    }

    @Test
    void testGetOrInsertLookup_DifferentValues_InsertSeparately() throws SQLException {
        String value1 = "NameOne";
        String value2 = "NameTwo";

        Integer id1 = databaseManager.getOrInsertLookup(connection, "wheel_side", value1);
        Integer id2 = databaseManager.getOrInsertLookup(connection, "wheel_side", value2);

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM wheel_side WHERE name IN (?, ?)")) {
            stmt.setString(1, value1);
            stmt.setString(2, value2);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }
        }
    }
}