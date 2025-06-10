package scraper.database;

import scraper.model.Cars;
import scraper.model.CarDetails;
import scraper.model.LookupEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import static scraper.database.DatabaseUtils.setNullableString;
import static scraper.database.DatabaseUtils.setNullableInt;
import static scraper.database.DatabaseUtils.parseRomanianDate;

class CarsMapper implements EntityMapper<Cars> {
    private final ParticularitiesMapper particularitiesMapper;

    public CarsMapper() {
        this.particularitiesMapper = new ParticularitiesMapper();
    }

    public void prepareCarStatement(Cars car, PreparedStatement stmt, String cleanLink, long particularitiesId,
                                    Connection conn) throws SQLException {
        setNullableString(stmt, 1, cleanLink);
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
        LookupEntityMapper adTypeMapper = new LookupEntityMapper("ad_type", adTypeName);
        Integer adTypeId = adTypeMapper.getOrInsertLookup(conn);
        setNullableInt(stmt, 6, adTypeId);
        stmt.setLong(7, particularitiesId);
    }

    @Override
    public Cars map(CarDetails carDetails) {
        Cars cars = new Cars();
        cars.setLink(carDetails.getLink());
        cars.setRegion(carDetails.getRegion());
        cars.setMileage(carDetails.getMileage());
        cars.setPriceEur(carDetails.getEurPrice());
        cars.setUpdateDate(carDetails.getUpdateDate());
        cars.setAdType(carDetails.getAdType() != null ? new LookupEntity(carDetails.getAdType()) : null);
        cars.setParticularities(particularitiesMapper.map(carDetails));

        return cars;
    }

    @Override
    public void save(CarDetails carDetails, Connection conn) throws SQLException {
        Cars car = map(carDetails);
        long particularitiesId = particularitiesMapper.saveAndReturnId(carDetails, conn);
        String cleanLink = car.getLink().split("\\?")[0];
        String insertSql = """
            INSERT INTO cars (
                link, region, mileage, price_eur, update_date, ad_type_id, particularities_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (link) DO NOTHING
            """;
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            prepareCarStatement(car, stmt, cleanLink, particularitiesId, conn);
            stmt.executeUpdate();
        }
    }

    @Override
    public void saveBatch(List<CarDetails> carDetailsList, Connection conn) throws SQLException {
        String insertSql = """
            INSERT INTO cars (
                link, region, mileage, price_eur, update_date, ad_type_id, particularities_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (link) DO NOTHING
            """;
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            for (CarDetails carDetails : carDetailsList) {
                Cars car = map(carDetails);
                long particularitiesId = particularitiesMapper.saveAndReturnId(carDetails, conn);
                String cleanLink = car.getLink().split("\\?")[0];
                prepareCarStatement(car, stmt, cleanLink, particularitiesId, conn);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}