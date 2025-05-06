import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.postgresql.util.PSQLException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/scraper_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "pass";

    public void saveCars(List<CarDetails> finalProducts) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);

            String particularitiesSql = "INSERT INTO particularities (" +
                    "author, year_of_fabrication, wheel_side, nr_of_seats, body, nr_of_doors, " +
                    "engine_capacity_cm3, horsepower, petrol_type, gears_type, traction_type, color) " +
                    "VALUES (?, ?, ?::wheel_side_enum, ?, ?, ?, ?, ?, ?::petrol_type_enum, ?::gears_type_enum, ?::traction_type_enum, ?) RETURNING id";

            String carsSql = "INSERT INTO cars (" +
                    "link, region, mileage, price_eur, update_date, ad_type, particularities_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?::ad_type_enum, ?) ON CONFLICT (link) DO NOTHING";

            try (PreparedStatement particularitiesStmt = conn.prepareStatement(particularitiesSql);
                 PreparedStatement carsStmt = conn.prepareStatement(carsSql)) {

                for (CarDetails car : finalProducts) {
                    String cleanLink = car.getLink();
                    if (cleanLink.contains("?clickToken")) {
                        int queryIndex = cleanLink.indexOf('?');
                        if (queryIndex != -1) {
                            cleanLink = cleanLink.substring(0, queryIndex);
                        }
                    }

                    setNullableString(particularitiesStmt, 1, car.getAuthor());
                    setNullableInt(particularitiesStmt, 2, car.getYearOfFabrication());
                    setNullableString(particularitiesStmt, 3, car.getWheelSide());
                    setNullableInt(particularitiesStmt, 4, car.getNrOfSeats());
                    setNullableString(particularitiesStmt, 5, car.getBody());
                    setNullableInt(particularitiesStmt, 6, car.getNrOfDoors());
                    setNullableInt(particularitiesStmt, 7, car.getEngineCapacity());
                    setNullableInt(particularitiesStmt, 8, car.getHorsepower());
                    setNullableString(particularitiesStmt, 9, car.getPetrolType());
                    setNullableString(particularitiesStmt, 10, car.getGearsType());
                    setNullableString(particularitiesStmt, 11, car.getTractionType());
                    setNullableString(particularitiesStmt, 12, car.getColor());

                    try {
                        ResultSet rs = particularitiesStmt.executeQuery();
                        if (rs.next()) {
                            long particularitiesId = rs.getLong("id");
                            carsStmt.setString(1, cleanLink);
                            setNullableString(carsStmt, 2, car.getRegion());
                            setNullableInt(carsStmt, 3, car.getMileage());
                            setNullableInt(carsStmt, 4, car.getEurPrice());
                            Timestamp timestamp = parseRomanianDate(car.getUpdateDate());
                            if (timestamp == null) {
                                carsStmt.setNull(5, java.sql.Types.TIMESTAMP);
                            } else {
                                carsStmt.setTimestamp(5, timestamp);
                            }
                            setNullableString(carsStmt, 6, car.getAdType());
                            carsStmt.setLong(7, particularitiesId);

                            carsStmt.addBatch();
                        }
                    } catch (PSQLException e) {
                        if (e.getSQLState().equals("23503")) {
                            System.err.println("Enum value mismatch for car: " + cleanLink + " - " + e.getMessage());
                            continue;
                        }
                        throw e;
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

}