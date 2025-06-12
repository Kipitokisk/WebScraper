package scraper.database;

import scraper.database.registry.LookupEntityRegistry;
import scraper.database.registry.ParticularitiesRegistry;
import scraper.model.*;
import scraper.model.lookupEntity.AdType;

import java.sql.*;
import java.util.List;

import static scraper.database.DatabaseUtils.*;

public class CarsMapper implements EntityMapper<Cars> {
    private static final String INSERT_SQL = """
        INSERT INTO cars (
            link, region, mileage, price_eur, update_date, ad_type_id, particularities_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (link) DO NOTHING
        """;

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
}