package scraper.database;

import scraper.database.registry.LookupEntityRegistry;
import scraper.database.registry.ParticularitiesRegistry;
import scraper.model.*;
import scraper.model.lookup.AdType;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static scraper.database.DatabaseUtils.*;

public class CarsMapper implements EntityMapper<Cars> {
    private static final String INSERT_SQL = """
        INSERT INTO cars (
            link, region, mileage, price_eur, update_date, ad_type_id, particularities_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (link) DO NOTHING
        """;

    private static final String INSERT_TEMP_LINKS_SQL = "INSERT INTO temp_links (link) VALUES(?)";
    private static final String SELECT_NEW_LINKS_SQL = "SELECT link FROM temp_links WHERE link NOT IN (SELECT link FROM cars)";

    private final DatabaseManager dbManager;
    private final LookupEntityRegistry lookupRegistry;
    private final ParticularitiesRegistry particularitiesRegistry;

    public CarsMapper(LookupEntityRegistry lookupRegistry, ParticularitiesRegistry particularitiesRegistry, DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.lookupRegistry = lookupRegistry;
        this.particularitiesRegistry = particularitiesRegistry;
    }

    private void prepareCarStatement(Cars car, PreparedStatement stmt) throws SQLException {
        setNullableString(stmt, 1, car.getLink());
        setNullableString(stmt, 2, car.getRegion());
        setNullableInt(stmt, 3, car.getMileage());
        setNullableInt(stmt, 4, car.getPriceEur());

        Timestamp timestamp = parseRomanianDate(car.getUpdateDate());
        if (timestamp == null) {
            stmt.setNull(5, Types.TIMESTAMP);
        } else {
            stmt.setTimestamp(5, timestamp);
        }

        String adTypeName = car.getAdType() != null ? car.getAdType().getName() : null;
        Integer adTypeId = lookupRegistry.getAdTypeId(adTypeName);
        setNullableInt(stmt, 6, adTypeId);
        stmt.setLong(7, car.getParticularities());
    }

    @Override
    public Cars map(CarDetails carDetails) {
        Cars cars = new Cars();
        cars.setLink(carDetails.getLink());
        cars.setRegion(carDetails.getRegion());
        cars.setMileage(carDetails.getMileage());
        cars.setPriceEur(carDetails.getEurPrice());
        cars.setUpdateDate(carDetails.getUpdateDate());
        cars.setAdType(carDetails.getAdType() != null ? new AdType(carDetails.getAdType()) : null);
        cars.setParticularities(particularitiesRegistry.getParticularitiesId(carDetails.getLink()));
        return cars;
    }

    @Override
    public void saveBatch(List<CarDetails> carDetailsList) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, INSERT_SQL)) {
            for (CarDetails carDetails : carDetailsList) {
                Cars car = map(carDetails);
                prepareCarStatement(car, stmt);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public static List<String> extractLinks(DatabaseManager databaseManager, List<String> allLinks) {
        List<String> newLinks = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection()) {

            try (Statement statement = conn.createStatement()){
                statement.execute("CREATE TEMP TABLE temp_links (link TEXT) ON COMMIT PRESERVE ROWS");
            }

            try (PreparedStatement stmt = databaseManager.prepareStatement(conn, INSERT_TEMP_LINKS_SQL)){
                for (String link : allLinks) {
                    stmt.setString(1, link);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            try (PreparedStatement selectStmt = conn.prepareStatement(SELECT_NEW_LINKS_SQL);
                 ResultSet rs = selectStmt.executeQuery()) {

                while (rs.next()) {
                    newLinks.add(rs.getString("link"));
                }
            }

            return newLinks;
        } catch (SQLException e) {
            throw new RuntimeException("Error extracting car links", e);
        }
    }

    public static double getAveragePrice(int minMileage, int maxMileage, DatabaseManager dbManager) throws SQLException {
        String sql = """
        SELECT AVG(price_eur)
        FROM cars
        WHERE ad_type_id = (SELECT id FROM ad_type WHERE name = 'Vând')
          AND price_eur IS NOT NULL
          AND mileage IS NOT NULL
          AND mileage > ?
          AND mileage < ?
    """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, sql)) {

            stmt.setInt(1, minMileage);
            stmt.setInt(2, maxMileage);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    if (rs.wasNull()) {
                        throw new RuntimeException("Cannot compute average - DB returned null");
                    }
                    return avg;
                } else {
                    throw new RuntimeException("Cannot compute average - no result");
                }
            }
        }
    }

    public static int getMinPrice(DatabaseManager dbManager) throws SQLException {
        String sql = """
        SELECT MIN(price_eur)
        FROM cars
        WHERE ad_type_id = (SELECT id FROM ad_type WHERE name = 'Vând')
        AND price_eur IS NOT NULL
    """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new RuntimeException("No min price found in DB");
                }
            }
        }
    }

    public static int getMaxPrice(DatabaseManager dbManager) throws SQLException {
        String sql = """
        SELECT MAX(price_eur)
        FROM cars
        WHERE ad_type_id = (SELECT id FROM ad_type WHERE name = 'Vând')
        AND price_eur IS NOT NULL
    """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new RuntimeException("No max price found in DB");
                }
            }
        }
    }
}