package scraper.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import scraper.model.CarDetails;
import scraper.model.Particularities;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ParticularitiesMapperIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withInitScript("init.sql");

    private DatabaseManager dbManager;
    private ParticularitiesMapper particularitiesMapper;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        dbManager = new DatabaseManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        particularitiesMapper = new ParticularitiesMapper(dbManager);
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
    void testMap_ValidCarDetails() {
        CarDetails carDetails = new CarDetails.Builder()
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

        Particularities p = particularitiesMapper.map(carDetails);

        assertNotNull(p, "Mapped Particularities should not be null");
        assertEquals("John", p.getAuthor(), "Author should match");
        assertEquals(2016, p.getYearOfFabrication(), "Year of fabrication should match");
        assertEquals("Stânga", p.getWheelSide().getName(), "Wheel side should match");
        assertEquals("5", p.getNrOfSeats().getName(), "Number of seats should match");
        assertEquals("Sedan", p.getBody().getName(), "Body should match");
        assertEquals("4", p.getNrOfDoors().getName(), "Number of doors should match");
        assertEquals("1800", p.getEngineCapacity().getName(), "Engine capacity should match");
        assertEquals("132", p.getHorsepower().getName(), "Horsepower should match");
        assertEquals("Benzină", p.getPetrolType().getName(), "Petrol type should match");
        assertEquals("Automat", p.getGearsType().getName(), "Gears type should match");
        assertEquals("Față", p.getTractionType().getName(), "Traction type should match");
        assertEquals("Alb", p.getColor().getName(), "Color should match");
        assertNull(p.getId(), "ID should be null initially");
    }

    @Test
    void testMap_NullFields() {
        CarDetails carDetails = new CarDetails.Builder().build(); // No fields set
        Particularities p = particularitiesMapper.map(carDetails);

        assertNotNull(p, "Mapped Particularities should not be null");
        assertNull(p.getAuthor(), "Author should be null");
        assertNull(p.getYearOfFabrication(), "Year of fabrication should be null");
        assertNull(p.getWheelSide(), "Wheel side should be null");
        assertNull(p.getNrOfSeats(), "Number of seats should be null");
        assertNull(p.getBody(), "Body should be null");
        assertNull(p.getNrOfDoors(), "Number of doors should be null");
        assertNull(p.getEngineCapacity(), "Engine capacity should be null");
        assertNull(p.getHorsepower(), "Horsepower should be null");
        assertNull(p.getPetrolType(), "Petrol type should be null");
        assertNull(p.getGearsType(), "Gears type should be null");
        assertNull(p.getTractionType(), "Traction type should be null");
        assertNull(p.getColor(), "Color should be null");
        assertNull(p.getId(), "ID should be null");
    }

    @Test
    void testSave_ValidCarDetails() throws SQLException {
        CarDetails carDetails = new CarDetails.Builder()
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

        particularitiesMapper.save(carDetails);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT p.author, p.year_of_fabrication, ws.name AS wheel_side, ns.name AS nr_of_seats,
                            b.name AS body, nd.name AS nr_of_doors, ec.name AS engine_capacity,
                            hp.name AS horsepower, pt.name AS petrol_type, gt.name AS gears_type,
                            tt.name AS traction_type, cl.name AS color
                     FROM particularities p
                     LEFT JOIN wheel_side ws ON p.wheel_side_id = ws.id
                     LEFT JOIN nr_of_seats ns ON p.nr_of_seats_id = ns.id
                     LEFT JOIN body b ON p.body_id = b.id
                     LEFT JOIN nr_of_doors nd ON p.nr_of_doors_id = nd.id
                     LEFT JOIN engine_capacity ec ON p.engine_capacity_id = ec.id
                     LEFT JOIN horsepower hp ON p.horsepower_id = hp.id
                     LEFT JOIN petrol_type pt ON p.petrol_type_id = pt.id
                     LEFT JOIN gears_type gt ON p.gears_type_id = gt.id
                     LEFT JOIN traction_type tt ON p.traction_type_id = tt.id
                     LEFT JOIN color cl ON p.color_id = cl.id
                     """)) {
            assertTrue(rs.next(), "A record should be inserted");
            assertEquals("John", rs.getString("author"), "Author should match");
            assertEquals(2016, rs.getInt("year_of_fabrication"), "Year of fabrication should match");
            assertEquals("Stânga", rs.getString("wheel_side"), "Wheel side should match");
            assertEquals("5", rs.getString("nr_of_seats"), "Number of seats should match");
            assertEquals("Sedan", rs.getString("body"), "Body should match");
            assertEquals("4", rs.getString("nr_of_doors"), "Number of doors should match");
            assertEquals("1800", rs.getString("engine_capacity"), "Engine capacity should match");
            assertEquals("132", rs.getString("horsepower"), "Horsepower should match");
            assertEquals("Benzină", rs.getString("petrol_type"), "Petrol type should match");
            assertEquals("Automat", rs.getString("gears_type"), "Gears type should match");
            assertEquals("Față", rs.getString("traction_type"), "Traction type should match");
            assertEquals("Alb", rs.getString("color"), "Color should match");
        }
    }

    @Test
    void testSave_NullFields() throws SQLException {
        CarDetails carDetails = new CarDetails.Builder().build(); // No fields set
        particularitiesMapper.save(carDetails);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM particularities")) {
            assertTrue(rs.next(), "A record should be inserted");
            assertNull(rs.getString("author"), "Author should be null");
            assertEquals(0, rs.getInt("year_of_fabrication"), "Year of fabrication should be 0 (default for INTEGER)");
            assertNull(rs.getObject("wheel_side_id"), "Wheel side ID should be null");
            assertNull(rs.getObject("nr_of_seats_id"), "Number of seats ID should be null");
            assertNull(rs.getObject("body_id"), "Body ID should be null");
            assertNull(rs.getObject("nr_of_doors_id"), "Number of doors ID should be null");
            assertNull(rs.getObject("engine_capacity_id"), "Engine capacity ID should be null");
            assertNull(rs.getObject("horsepower_id"), "Horsepower ID should be null");
            assertNull(rs.getObject("petrol_type_id"), "Petrol type ID should be null");
            assertNull(rs.getObject("gears_type_id"), "Gears type ID should be null");
            assertNull(rs.getObject("traction_type_id"), "Traction type ID should be null");
            assertNull(rs.getObject("color_id"), "Color ID should be null");
        }
    }

    @Test
    void testSaveBatch_MultipleCarDetails() throws SQLException {
        CarDetails car1 = new CarDetails.Builder()
                .author("John")
                .yearOfFabrication(2016)
                .wheelSide("Stânga")
                .nrOfSeats(5)
                .body("Sedan")
                .build();
        CarDetails car2 = new CarDetails.Builder()
                .author("Jane")
                .yearOfFabrication(2018)
                .wheelSide("Stânga")
                .nrOfSeats(7)
                .body("SUV")
                .build();

        particularitiesMapper.saveBatch(Arrays.asList(car1, car2));

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT p.author, p.year_of_fabrication, ws.name AS wheel_side, ns.name AS nr_of_seats,
                            b.name AS body
                     FROM particularities p
                     LEFT JOIN wheel_side ws ON p.wheel_side_id = ws.id
                     LEFT JOIN nr_of_seats ns ON p.nr_of_seats_id = ns.id
                     LEFT JOIN body b ON p.body_id = b.id
                     ORDER BY p.year_of_fabrication
                     """)) {
            assertTrue(rs.next(), "First record should exist");
            assertEquals("John", rs.getString("author"), "First author should match");
            assertEquals(2016, rs.getInt("year_of_fabrication"), "First year should match");
            assertEquals("Stânga", rs.getString("wheel_side"), "First wheel side should match");
            assertEquals("5", rs.getString("nr_of_seats"), "First number of seats should match");
            assertEquals("Sedan", rs.getString("body"), "First body should match");

            assertTrue(rs.next(), "Second record should exist");
            assertEquals("Jane", rs.getString("author"), "Second author should match");
            assertEquals(2018, rs.getInt("year_of_fabrication"), "Second year should match");
            assertEquals("Stânga", rs.getString("wheel_side"), "Second wheel side should match");
            assertEquals("7", rs.getString("nr_of_seats"), "Second number of seats should match");
            assertEquals("SUV", rs.getString("body"), "Second body should match");
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM wheel_side WHERE name = 'Stânga'")) {
            assertTrue(rs.next(), "Should have a result");
            assertEquals(1, rs.getInt("count"), "Wheel side 'Stânga' should appear once due to ON CONFLICT");
        }
    }

    @Test
    void testSaveBatch_EmptyList() throws SQLException {
        particularitiesMapper.saveBatch(Collections.emptyList());

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM particularities")) {
            assertTrue(rs.next(), "Should have a result");
            assertEquals(0, rs.getInt("count"), "No records should be inserted");
        }
    }

    @Test
    void testSaveAndReturnId_ValidCarDetails() throws SQLException {
        CarDetails carDetails = new CarDetails.Builder()
                .author("John")
                .yearOfFabrication(2016)
                .wheelSide("Stânga")
                .build();

        try (Connection conn = dbManager.getConnection()) {
            long id = particularitiesMapper.saveAndReturnId(carDetails, conn);
            assertTrue(id > 0, "Returned ID should be positive");

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT p.id, author, year_of_fabrication, ws.name AS wheel_side " +
                         "FROM particularities p LEFT JOIN wheel_side ws ON p.wheel_side_id = ws.id " +
                         "WHERE p.id = " + id)){
                assertTrue(rs.next(), "Record should exist");
                assertEquals(id, rs.getInt("id"), "ID should match");
                assertEquals("John", rs.getString("author"), "Author should match");
                assertEquals(2016, rs.getInt("year_of_fabrication"), "Year should match");
                assertEquals("Stânga", rs.getString("wheel_side"), "Wheel side should match");
            }
        }
    }

    @Test
    void testSaveAndReturnId_NullFields() throws SQLException {
        CarDetails carDetails = new CarDetails.Builder().build();
        try (Connection conn = dbManager.getConnection()) {
            long id = particularitiesMapper.saveAndReturnId(carDetails, conn);
            assertTrue(id > 0, "Returned ID should be positive");

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, author, year_of_fabrication FROM particularities WHERE id = " + id)) {
                assertTrue(rs.next(), "Record should exist");
                assertEquals(id, rs.getInt("id"), "ID should match");
                assertNull(rs.getString("author"), "Author should be null");
                assertEquals(0, rs.getInt("year_of_fabrication"), "Year should be 0 (default for INTEGER)");
            }
        }
    }
}