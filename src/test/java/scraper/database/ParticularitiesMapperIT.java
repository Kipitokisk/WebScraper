package scraper.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import scraper.model.CarDetails;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ParticularitiesMapperIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("scraper_db")
            .withUsername("postgres")
            .withPassword("pass")
            .withInitScript("init.sql");

    private ParticularitiesMapper particularitiesMapper;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        particularitiesMapper = new ParticularitiesMapper();
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
            stmt.execute("TRUNCATE TABLE particularities, wheel_side, nr_of_seats, body, " +
                    "nr_of_doors, engine_capacity, horsepower, petrol_type, gears_type, " +
                    "traction_type, color RESTART IDENTITY CASCADE");
            stmt.execute("SET CONSTRAINTS ALL IMMEDIATE");
        }
    }

    @Test
    void testSave_ValidParticularities() throws SQLException {
        CarDetails car = new CarDetails.Builder()
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
                .color("Alb")
                .build();

        particularitiesMapper.save(car, connection);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT p.author, p.year_of_fabrication,
                            ws.name as wheel_side, ns.name as nr_of_seats, b.name as body,
                            nd.name as nr_of_doors, ec.name as engine_capacity, hp.name as horsepower,
                            pt.name as petrol_type, gt.name as gears_type, tt.name as traction_type,
                            cl.name as color
                     FROM particularities p
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
                     """)) {

            assertTrue(rs.next());
            assertEquals("John", rs.getString("author"));
            assertEquals(2016, rs.getInt("year_of_fabrication"));
            assertEquals("Stânga", rs.getString("wheel_side"));
            assertEquals("5", rs.getString("nr_of_seats"));
            assertEquals("Sedan", rs.getString("body"));
            assertEquals("4", rs.getString("nr_of_doors"));
            assertEquals("1800", rs.getString("engine_capacity"));
            assertEquals("132", rs.getString("horsepower"));
            assertEquals("Benzină", rs.getString("petrol_type"));
            assertEquals("Automat", rs.getString("gears_type"));
            assertEquals("Față", rs.getString("traction_type"));
            assertEquals("Alb", rs.getString("color"));
        }
    }

    @Test
    void testSaveBatch_EmptyList() throws SQLException {
        particularitiesMapper.saveBatch(Collections.emptyList(), connection);
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM particularities")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("count"));
        }
    }
}