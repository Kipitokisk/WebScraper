package scraper.database;

import scraper.model.CarDetails;
import scraper.model.LookupEntity;
import scraper.model.Particularities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static scraper.database.DatabaseUtils.setNullableString;
import static scraper.database.DatabaseUtils.setNullableInt;

class ParticularitiesMapper implements EntityMapper<Particularities> {

    private static final String INSERT_SQL = """
        INSERT INTO particularities (
            author, year_of_fabrication, wheel_side_id, nr_of_seats_id, body_id, nr_of_doors_id,
            engine_capacity_id, horsepower_id, petrol_type_id,
            gears_type_id, traction_type_id, color_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
        """;

    private final DatabaseManager dbManager;

    public ParticularitiesMapper(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private void prepareParticularitiesStatement(Particularities particularities, PreparedStatement stmt, Connection conn) throws SQLException {
        String wheelSideName = getName(particularities.getWheelSide());
        String nrOfSeatsName = getName(particularities.getNrOfSeats());
        String bodyName = getName(particularities.getBody());
        String nrOfDoorsName = getName(particularities.getNrOfDoors());
        String engineCapacityName = getName(particularities.getEngineCapacity());
        String horsepowerName = getName(particularities.getHorsepower());
        String petrolTypeName = getName(particularities.getPetrolType());
        String gearsTypeName = getName(particularities.getGearsType());
        String tractionTypeName = getName(particularities.getTractionType());
        String colorName = getName(particularities.getColor());

        Integer wheelSideId = new LookupEntityMapper("wheel_side", dbManager).getOrInsertLookup(wheelSideName);
        Integer nrOfSeatsId = new LookupEntityMapper("nr_of_seats", dbManager).getOrInsertLookup(nrOfSeatsName);
        Integer bodyId = new LookupEntityMapper("body", dbManager).getOrInsertLookup(bodyName);
        Integer nrOfDoorsId = new LookupEntityMapper("nr_of_doors", dbManager).getOrInsertLookup(nrOfDoorsName);
        Integer engineCapacityId = new LookupEntityMapper("engine_capacity", dbManager).getOrInsertLookup(engineCapacityName);
        Integer horsepowerId = new LookupEntityMapper("horsepower", dbManager).getOrInsertLookup(horsepowerName);
        Integer petrolTypeId = new LookupEntityMapper("petrol_type", dbManager).getOrInsertLookup(petrolTypeName);
        Integer gearsTypeId = new LookupEntityMapper("gears_type", dbManager).getOrInsertLookup(gearsTypeName);
        Integer tractionTypeId = new LookupEntityMapper("traction_type", dbManager).getOrInsertLookup(tractionTypeName);
        Integer colorId = new LookupEntityMapper("color", dbManager).getOrInsertLookup(colorName);

        setNullableString(stmt, 1, particularities.getAuthor());
        setNullableInt(stmt, 2, particularities.getYearOfFabrication());
        setNullableInt(stmt, 3, wheelSideId);
        setNullableInt(stmt, 4, nrOfSeatsId);
        setNullableInt(stmt, 5, bodyId);
        setNullableInt(stmt, 6, nrOfDoorsId);
        setNullableInt(stmt, 7, engineCapacityId);
        setNullableInt(stmt, 8, horsepowerId);
        setNullableInt(stmt, 9, petrolTypeId);
        setNullableInt(stmt, 10, gearsTypeId);
        setNullableInt(stmt, 11, tractionTypeId);
        setNullableInt(stmt, 12, colorId);
    }

    private String getName(LookupEntity entity) {
        return entity != null ? String.valueOf(entity.getName()) : null;
    }

    @Override
    public Particularities map(CarDetails carDetails) {
        Particularities p = new Particularities();
        p.setAuthor(carDetails.getAuthor());
        p.setYearOfFabrication(carDetails.getYearOfFabrication());
        p.setWheelSide(toEntity(carDetails.getWheelSide()));
        p.setNrOfSeats(toEntity(carDetails.getNrOfSeats()));
        p.setBody(toEntity(carDetails.getBody()));
        p.setNrOfDoors(toEntity(carDetails.getNrOfDoors()));
        p.setEngineCapacity(toEntity(carDetails.getEngineCapacity()));
        p.setHorsepower(toEntity(carDetails.getHorsepower()));
        p.setPetrolType(toEntity(carDetails.getPetrolType()));
        p.setGearsType(toEntity(carDetails.getGearsType()));
        p.setTractionType(toEntity(carDetails.getTractionType()));
        p.setColor(toEntity(carDetails.getColor()));
        return p;
    }

    private LookupEntity toEntity(Object val) {
        return val != null ? new LookupEntity(String.valueOf(val)) : null;
    }

    @Override
    public void save(CarDetails carDetails) throws SQLException {
        Particularities p = map(carDetails);
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, INSERT_SQL)){
            prepareParticularitiesStatement(p, stmt, conn);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                p.setId(rs.getInt("id"));
            }
        }
    }

    @Override
    public void saveBatch(List<CarDetails> carDetailsList) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, INSERT_SQL)){
            for (CarDetails carDetails : carDetailsList) {
                Particularities p = map(carDetails);
                prepareParticularitiesStatement(p, stmt, conn);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public long saveAndReturnId(CarDetails carDetails, Connection conn) throws SQLException {
        Particularities p = map(carDetails);
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            prepareParticularitiesStatement(p, stmt, conn);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                p.setId(rs.getInt("id"));
                return p.getId();
            }
        }
        return -1;
    }
}
