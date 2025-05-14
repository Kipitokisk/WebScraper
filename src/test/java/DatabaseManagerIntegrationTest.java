import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DatabaseManagerIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("scraper_db")
            .withUsername("postgres")
            .withPassword("pass");

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

        createSchema();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    private void createSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS cars CASCADE");
            stmt.execute("DROP TABLE IF EXISTS particularities CASCADE");
            stmt.execute("DROP TABLE IF EXISTS wheel_side CASCADE");
            stmt.execute("DROP TABLE IF EXISTS nr_of_seats CASCADE");
            stmt.execute("DROP TABLE IF EXISTS body CASCADE");
            stmt.execute("DROP TABLE IF EXISTS nr_of_doors CASCADE");
            stmt.execute("DROP TABLE IF EXISTS engine_capacity CASCADE");
            stmt.execute("DROP TABLE IF EXISTS horsepower CASCADE");
            stmt.execute("DROP TABLE IF EXISTS petrol_type CASCADE");
            stmt.execute("DROP TABLE IF EXISTS gears_type CASCADE");
            stmt.execute("DROP TABLE IF EXISTS traction_type CASCADE");
            stmt.execute("DROP TABLE IF EXISTS color CASCADE");
            stmt.execute("DROP TABLE IF EXISTS ad_type CASCADE");

            stmt.execute("CREATE TABLE wheel_side (id SERIAL PRIMARY KEY, name VARCHAR(50) UNIQUE)");
            stmt.execute("CREATE TABLE nr_of_seats (id SERIAL PRIMARY KEY, name INTEGER UNIQUE)");
            stmt.execute("CREATE TABLE body (id SERIAL PRIMARY KEY, name VARCHAR(50) UNIQUE)");
            stmt.execute("CREATE TABLE nr_of_doors (id SERIAL PRIMARY KEY, name INTEGER UNIQUE)");
            stmt.execute("CREATE TABLE engine_capacity (id SERIAL PRIMARY KEY, name INTEGER UNIQUE)");
            stmt.execute("CREATE TABLE horsepower (id SERIAL PRIMARY KEY, name INTEGER UNIQUE)");
            stmt.execute("CREATE TABLE petrol_type (id SERIAL PRIMARY KEY, name VARCHAR(50) UNIQUE)");
            stmt.execute("CREATE TABLE gears_type (id SERIAL PRIMARY KEY, name VARCHAR(50) UNIQUE)");
            stmt.execute("CREATE TABLE traction_type (id SERIAL PRIMARY KEY, name VARCHAR(50) UNIQUE)");
            stmt.execute("CREATE TABLE color (id SERIAL PRIMARY KEY, name VARCHAR(50) UNIQUE)");
            stmt.execute("CREATE TABLE ad_type (id SERIAL PRIMARY KEY, name VARCHAR(50) UNIQUE)");

            stmt.execute("""
                    CREATE TABLE particularities (
                        id SERIAL PRIMARY KEY,
                        author VARCHAR(100),
                        year_of_fabrication INTEGER,
                        wheel_side_id INTEGER REFERENCES wheel_side(id),
                        nr_of_seats_id INTEGER REFERENCES nr_of_seats(id),
                        body_id INTEGER REFERENCES body(id),
                        nr_of_doors_id INTEGER REFERENCES nr_of_doors(id),
                        engine_capacity_id INTEGER REFERENCES engine_capacity(id),
                        horsepower_id INTEGER REFERENCES horsepower(id),
                        petrol_type_id INTEGER REFERENCES petrol_type(id),
                        gears_type_id INTEGER REFERENCES gears_type(id),
                        traction_type_id INTEGER REFERENCES traction_type(id),
                        color_id INTEGER REFERENCES color(id)
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE cars (
                        id SERIAL PRIMARY KEY,
                        link VARCHAR(255) UNIQUE,
                        region VARCHAR(100),
                        mileage INTEGER,
                        price_eur INTEGER,
                        update_date TIMESTAMP,
                        ad_type_id INTEGER REFERENCES ad_type(id),
                        particularities_id INTEGER REFERENCES particularities(id)
                    )
                    """);
        }
    }

    @Test
    void testSaveCars() throws SQLException {
        CarDetails car = new CarDetails(
                "https://999.md/ro/car", "Renault Megane 2016", 15000, 100000, "2023-10-01",
                "Vând", "Orhei", "John", 2016, "Stânga", 5, "Sedan", 4, 1800, 132,
                "Benzină", "Automat", "Față", "Alb"
        );

        databaseManager.saveCars(Collections.singletonList(car));

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT c.*, p.*
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
                     JOIN ad_type at ON c.ad_type_id = at.id
                     """)) {

            assertTrue(rs.next());
            assertEquals("https://999.md/ro/car", rs.getString("link"));
            assertEquals("Orhei", rs.getString("region"));
            assertEquals(100000, rs.getInt("mileage"));
            assertEquals(15000, rs.getInt("price_eur"));
            assertEquals("John", rs.getString("author"));
            assertEquals(2016, rs.getInt("year_of_fabrication"));
            assertEquals(1, rs.getInt("nr_of_seats_id"));
            assertEquals(1, rs.getInt("body_id"));
            assertEquals(1, rs.getInt("nr_of_doors_id"));
            assertEquals(1, rs.getInt("engine_capacity_id"));
            assertEquals(1, rs.getInt("horsepower_id"));
            assertEquals(1, rs.getInt("petrol_type_id"));
            assertEquals(1, rs.getInt("gears_type_id"));
            assertEquals(1, rs.getInt("traction_type_id"));
            assertEquals(1, rs.getInt("color_id"));
            assertEquals(1, rs.getInt("ad_type_id"));
        }
    }

    @Test
    void testSaveCarsWithNullValues() throws SQLException {

        CarDetails car = new CarDetails("https://999.md/ro/car", null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null
        );

        databaseManager.saveCars(Collections.singletonList(car));

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM cars JOIN particularities ON cars.particularities_id = particularities.id")) {
            assertTrue(rs.next());
            assertEquals("https://999.md/ro/car", rs.getString("link"));
            assertNull(rs.getString("region"));
            assertEquals(0, rs.getInt("mileage"));
            assertEquals(0, rs.getInt("price_eur"));
            assertNull(rs.getTimestamp("update_date"));
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
}