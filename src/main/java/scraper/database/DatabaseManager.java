package scraper.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.logic.DatabaseManagerHelper;
import scraper.model.CarDetails;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseManager {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final DatabaseManagerHelper databaseManagerHelper = new DatabaseManagerHelper();

    public DatabaseManager(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public void saveCars(List<CarDetails> finalProducts) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false);

            String insertParticularitiesSql = """
            INSERT INTO particularities (
                author, year_of_fabrication, wheel_side_id, nr_of_seats_id, body_id, nr_of_doors_id,
                engine_capacity_id, horsepower_id, petrol_type_id,
                gears_type_id, traction_type_id, color_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
        """;

            String insertCarSql = """
            INSERT INTO cars (
                link, region, mileage, price_eur, update_date, ad_type_id, particularities_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (link) DO NOTHING
        """;

            try (
                    PreparedStatement particularitiesStmt = conn.prepareStatement(insertParticularitiesSql);
                    PreparedStatement carsStmt = conn.prepareStatement(insertCarSql)
            ) {
                for (CarDetails car : finalProducts) {
                    String cleanLink = car.getLink().split("\\?")[0];

                    Integer wheelSideId = getOrInsertLookup(conn, "wheel_side", car.getWheelSide());
                    Integer nrOfSeatsId = getOrInsertLookup(conn, "nr_of_seats", car.getNrOfSeats());
                    Integer bodyId = getOrInsertLookup(conn, "body", car.getBody());
                    Integer nrOfDoorsId = getOrInsertLookup(conn, "nr_of_doors", car.getNrOfDoors());
                    Integer engineCapacityId = getOrInsertLookup(conn, "engine_capacity", car.getEngineCapacity());
                    Integer horsepowerId = getOrInsertLookup(conn, "horsepower", car.getHorsepower());
                    Integer petrolTypeId = getOrInsertLookup(conn, "petrol_type", car.getPetrolType());
                    Integer gearsTypeId = getOrInsertLookup(conn, "gears_type", car.getGearsType());
                    Integer tractionTypeId = getOrInsertLookup(conn, "traction_type", car.getTractionType());
                    Integer colorId = getOrInsertLookup(conn, "color", car.getColor());

                    setNullableString(particularitiesStmt, 1, car.getAuthor());
                    setNullableInt(particularitiesStmt, 2, car.getYearOfFabrication());
                    setNullableInt(particularitiesStmt, 3, wheelSideId);
                    setNullableInt(particularitiesStmt, 4, nrOfSeatsId);
                    setNullableInt(particularitiesStmt, 5, bodyId);
                    setNullableInt(particularitiesStmt, 6, nrOfDoorsId);
                    setNullableInt(particularitiesStmt, 7, engineCapacityId);
                    setNullableInt(particularitiesStmt, 8, horsepowerId);
                    setNullableInt(particularitiesStmt, 9, petrolTypeId);
                    setNullableInt(particularitiesStmt, 10, gearsTypeId);
                    setNullableInt(particularitiesStmt, 11, tractionTypeId);
                    setNullableInt(particularitiesStmt, 12, colorId);

                    ResultSet rs = particularitiesStmt.executeQuery();
                    if (rs.next()) {
                        long particularitiesId = rs.getLong("id");

                        carsStmt.setString(1, cleanLink);
                        setNullableString(carsStmt, 2, car.getRegion());
                        setNullableInt(carsStmt, 3, car.getMileage());
                        setNullableInt(carsStmt, 4, car.getEurPrice());
                        Timestamp timestamp = databaseManagerHelper.parseRomanianDate(car.getUpdateDate());
                        if (timestamp == null) {
                            carsStmt.setNull(5, Types.TIMESTAMP);
                        } else {
                            carsStmt.setTimestamp(5, timestamp);
                        }
                        Integer adTypeId = getOrInsertLookup(conn, "ad_type", car.getAdType());
                        setNullableInt(carsStmt, 6, adTypeId);
                        carsStmt.setLong(7, particularitiesId);

                        carsStmt.addBatch();
                    }
                }
                carsStmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    void setNullableString(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.VARCHAR);
        } else {
            stmt.setString(index, value);
        }
    }

    void setNullableInt(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }

    Integer getOrInsertLookup(Connection conn, String tableName, Object value) throws SQLException {
        if (value == null) return null;

        String stringValue = value.toString();

        String selectSql = "SELECT id FROM " + tableName + " WHERE \"name\" = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, stringValue);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }

        String insertSql = "INSERT INTO " + tableName + " (\"name\") VALUES (?) RETURNING id";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, stringValue);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }

        return null;
    }

}