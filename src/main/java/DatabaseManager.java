import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/scraper_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "pass";

    public void saveCars(List<CarDetails> finalProducts) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);

            String insertParticularitiesSql = """
            INSERT INTO particularities (
                wheel_side_id, nr_of_seats_id, body_id, nr_of_doors_id,
                engine_capacity_id, horsepower_id, petrol_type_id,
                gears_type_id, traction_type_id, color_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
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

                    setNullableInt(particularitiesStmt, 1, wheelSideId);
                    setNullableInt(particularitiesStmt, 2, nrOfSeatsId);
                    setNullableInt(particularitiesStmt, 3, bodyId);
                    setNullableInt(particularitiesStmt, 4, nrOfDoorsId);
                    setNullableInt(particularitiesStmt, 5, engineCapacityId);
                    setNullableInt(particularitiesStmt, 6, horsepowerId);
                    setNullableInt(particularitiesStmt, 7, petrolTypeId);
                    setNullableInt(particularitiesStmt, 8, gearsTypeId);
                    setNullableInt(particularitiesStmt, 9, tractionTypeId);
                    setNullableInt(particularitiesStmt, 10, colorId);

                    ResultSet rs = particularitiesStmt.executeQuery();
                    if (rs.next()) {
                        long particularitiesId = rs.getLong("id");

                        carsStmt.setString(1, cleanLink);
                        setNullableString(carsStmt, 2, car.getRegion());
                        setNullableInt(carsStmt, 3, car.getMileage());
                        setNullableInt(carsStmt, 4, car.getEurPrice());
                        Timestamp timestamp = parseRomanianDate(car.getUpdateDate());
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


    private void setNullableString(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.VARCHAR);
        } else {
            stmt.setString(index, value);
        }
    }

    private void setNullableInt(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }

    private Timestamp parseRomanianDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;

        String replaced = dateStr
                .replace("ian.", "Jan")
                .replace("feb.", "Feb")
                .replace("mar.", "Mar")
                .replace("apr.", "Apr")
                .replace("mai.", "May")
                .replace("iun.", "Jun")
                .replace("iul.", "Jul")
                .replace("aug.", "Aug")
                .replace("sept.", "Sep")
                .replace("oct.", "Oct")
                .replace("nov.", "Nov")
                .replace("dec.", "Dec");

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);
            Date parsed = sdf.parse(replaced);
            return new Timestamp(parsed.getTime());
        } catch (ParseException e) {
            System.err.println("Failed to parse normalized date: " + replaced);
            return null;
        }
    }

    private Integer getOrInsertLookup(Connection conn, String tableName, Object value) throws SQLException {
        if (value == null) return null;

        String valueType = value.getClass().getSimpleName();

        String selectSql = "SELECT id FROM " + tableName + " WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            if (valueType.equals("Integer")) {
                stmt.setInt(1, (Integer) value);
            } else if (valueType.equals("String")) {
                stmt.setString(1, value.toString());
            } else {
                throw new SQLException("Unsupported value type: " + valueType);
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }

        String insertSql = "INSERT INTO " + tableName + " (name) VALUES (?) RETURNING id";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            if (valueType.equals("Integer")) {
                stmt.setInt(1, (Integer) value);
            } else if (valueType.equals("String")) {
                stmt.setString(1, value.toString());
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }

        return null;
    }


}