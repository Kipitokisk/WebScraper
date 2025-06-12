//package scraper.database;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import scraper.model.CarDetails;
//
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.Collections;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@Testcontainers
//class CarsMapperIT {
//
//    @Container
//    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
//            .withDatabaseName("scraper_db")
//            .withUsername("postgres")
//            .withPassword("pass")
//            .withInitScript("init.sql");
//
//    private CarsMapper carsMapper;
//    private DatabaseManager dbManager;
//    private Connection connection;
//
//    @BeforeEach
//    void setUp() throws SQLException {
//        dbManager = new DatabaseManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
//        carsMapper = new CarsMapper(dbManager);
//        connection = dbManager.getConnection();
//        clearDatabase();
//    }
//
//    @AfterEach
//    void tearDown() throws SQLException {
//        if (connection != null && !connection.isClosed()) {
//            connection.close();
//        }
//    }
//
//    private void clearDatabase() throws SQLException {
//        try (Connection conn = dbManager.getConnection();
//             Statement stmt = conn.createStatement()) {
//            stmt.execute("SET CONSTRAINTS ALL DEFERRED");
//            stmt.execute("TRUNCATE TABLE cars, particularities, wheel_side, nr_of_seats, body, " +
//                    "nr_of_doors, engine_capacity, horsepower, petrol_type, gears_type, " +
//                    "traction_type, color, ad_type RESTART IDENTITY CASCADE");
//            stmt.execute("SET CONSTRAINTS ALL IMMEDIATE");
//        }
//    }
//
//    @Test
//    void testSave_ValidCar() throws SQLException {
//        CarDetails car = new CarDetails.Builder().link("https://999.md/ro/car")
//                .name("Renault Megane 2016")
//                .eurPrice(15000)
//                .mileage(100000)
//                .updateDate(null)
//                .adType("Vând")
//                .region("Orhei")
//                .author("John")
//                .yearOfFabrication(2016)
//                .wheelSide("Stânga")
//                .nrOfSeats("5")
//                .body("Sedan")
//                .nrOfDoors("4")
//                .engineCapacity("1800")
//                .horsepower("132")
//                .petrolType("Benzină")
//                .gearsType("Automat")
//                .tractionType("Față")
//                .color("Alb")
//                .build();
//
//        carsMapper.save(car);
//
//        try (Connection conn = dbManager.getConnection();
//             Statement stmt = conn.createStatement();
//             ResultSet rs = stmt.executeQuery("""
//                     SELECT c.link, c.region, c.mileage, c.price_eur, p.author, p.year_of_fabrication,
//                            c.update_date, ad.name as ad_type,
//                            ws.name as wheel_side, ns.name as nr_of_seats, b.name as body,
//                            nd.name as nr_of_doors, ec.name as engine_capacity, hp.name as horsepower,
//                            pt.name as petrol_type, gt.name as gears_type, tt.name as traction_type,
//                            cl.name as color
//                     FROM cars c
//                     JOIN particularities p ON c.particularities_id = p.id
//                     JOIN wheel_side ws ON p.wheel_side_id = ws.id
//                     JOIN nr_of_seats ns ON p.nr_of_seats_id = ns.id
//                     JOIN body b ON p.body_id = b.id
//                     JOIN nr_of_doors nd ON p.nr_of_doors_id = nd.id
//                     JOIN engine_capacity ec ON p.engine_capacity_id = ec.id
//                     JOIN horsepower hp ON p.horsepower_id = hp.id
//                     JOIN petrol_type pt ON p.petrol_type_id = pt.id
//                     JOIN gears_type gt ON p.gears_type_id = gt.id
//                     JOIN traction_type tt ON p.traction_type_id = tt.id
//                     JOIN color cl ON p.color_id = cl.id
//                     JOIN ad_type ad ON c.ad_type_id = ad.id
//                     """)) {
//
//            assertTrue(rs.next());
//            assertEquals("https://999.md/ro/car", rs.getString("link"));
//            assertEquals("Orhei", rs.getString("region"));
//            assertEquals(100000, rs.getInt("mileage"));
//            assertEquals(15000, rs.getInt("price_eur"));
//            assertEquals("John", rs.getString("author"));
//            assertEquals(2016, rs.getInt("year_of_fabrication"));
//            assertEquals("Vând", rs.getString("ad_type"));
//            assertEquals("Stânga", rs.getString("wheel_side"));
//            assertEquals("5", rs.getString("nr_of_seats"));
//            assertEquals("Sedan", rs.getString("body"));
//            assertEquals("4", rs.getString("nr_of_doors"));
//            assertEquals("1800", rs.getString("engine_capacity"));
//            assertEquals("132", rs.getString("horsepower"));
//            assertEquals("Benzină", rs.getString("petrol_type"));
//            assertEquals("Automat", rs.getString("gears_type"));
//            assertEquals("Față", rs.getString("traction_type"));
//            assertEquals("Alb", rs.getString("color"));
//        }
//    }
//
//    @Test
//    void testSaveBatch_EmptyList() throws SQLException {
//        carsMapper.saveBatch(Collections.emptyList());
//        try (Connection conn = dbManager.getConnection();
//             Statement stmt = conn.createStatement();
//             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM cars")) {
//            assertTrue(rs.next());
//            assertEquals(0, rs.getInt("count"));
//        }
//    }
//}