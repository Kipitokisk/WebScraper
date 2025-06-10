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

    public void prepareParticularitiesStatement(Particularities particularities, PreparedStatement stmt, Connection conn) throws SQLException {
        String wheelSideName = particularities.getWheelSide() != null ? particularities.getWheelSide().getName() : null;
        String nrOfSeatsName = particularities.getNrOfSeats() != null ? particularities.getNrOfSeats().getName() : null;
        String bodyName = particularities.getBody() != null ? particularities.getBody().getName() : null;
        String nrOfDoorsName = particularities.getNrOfDoors() != null ? particularities.getNrOfDoors().getName() : null;
        String engineCapacityName = particularities.getEngineCapacity() != null ? particularities.getEngineCapacity().getName() : null;
        String horsepowerName = particularities.getHorsepower() != null ? particularities.getHorsepower().getName() : null;
        String petrolTypeName = particularities.getPetrolType() != null ? particularities.getPetrolType().getName() : null;
        String gearsTypeName = particularities.getGearsType() != null ? particularities.getGearsType().getName() : null;
        String tractionTypeName = particularities.getTractionType() != null ? particularities.getTractionType().getName() : null;
        String colorName = particularities.getColor() != null ? particularities.getColor().getName() : null;

        LookupEntityMapper wheelSideMapper = new LookupEntityMapper("wheel_side", wheelSideName);
        LookupEntityMapper nrOfSeatsMapper = new LookupEntityMapper("nr_of_seats", nrOfSeatsName);
        LookupEntityMapper bodyMapper = new LookupEntityMapper("body", bodyName);
        LookupEntityMapper nrOfDoorsMapper = new LookupEntityMapper("nr_of_doors", nrOfDoorsName);
        LookupEntityMapper engineCapacityMapper = new LookupEntityMapper("engine_capacity", engineCapacityName);
        LookupEntityMapper horsepowerMapper = new LookupEntityMapper("horsepower", horsepowerName);
        LookupEntityMapper petrolTypeMapper = new LookupEntityMapper("petrol_type", petrolTypeName);
        LookupEntityMapper gearsTypeMapper = new LookupEntityMapper("gears_type", gearsTypeName);
        LookupEntityMapper tractionTypeMapper = new LookupEntityMapper("traction_type", tractionTypeName);
        LookupEntityMapper colorMapper = new LookupEntityMapper("color", colorName);

        Integer wheelSideId = wheelSideMapper.getOrInsertLookup(conn);
        Integer nrOfSeatsId = nrOfSeatsMapper.getOrInsertLookup(conn);
        Integer bodyId = bodyMapper.getOrInsertLookup(conn);
        Integer nrOfDoorsId = nrOfDoorsMapper.getOrInsertLookup(conn);
        Integer engineCapacityId = engineCapacityMapper.getOrInsertLookup(conn);
        Integer horsepowerId = horsepowerMapper.getOrInsertLookup(conn);
        Integer petrolTypeId = petrolTypeMapper.getOrInsertLookup(conn);
        Integer gearsTypeId = gearsTypeMapper.getOrInsertLookup(conn);
        Integer tractionTypeId = tractionTypeMapper.getOrInsertLookup(conn);
        Integer colorId = colorMapper.getOrInsertLookup(conn);

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


    @Override
    public Particularities map(CarDetails carDetails) {
        Particularities particularities = new Particularities();
        particularities.setAuthor(carDetails.getAuthor());
        particularities.setYearOfFabrication(carDetails.getYearOfFabrication());
        particularities.setWheelSide(carDetails.getWheelSide() != null ? new LookupEntity(carDetails.getWheelSide()) : null);
        particularities.setNrOfSeats(carDetails.getNrOfSeats() != null ? new LookupEntity(String.valueOf(carDetails.getNrOfSeats())) : null);
        particularities.setBody(carDetails.getBody() != null ? new LookupEntity(carDetails.getBody()) : null);
        particularities.setNrOfDoors(carDetails.getNrOfDoors() != null ? new LookupEntity(String.valueOf(carDetails.getNrOfDoors())) : null);
        particularities.setEngineCapacity(carDetails.getEngineCapacity() != null ? new LookupEntity(String.valueOf(carDetails.getEngineCapacity())) : null);
        particularities.setHorsepower(carDetails.getHorsepower() != null ? new LookupEntity(String.valueOf(carDetails.getHorsepower())) : null);
        particularities.setPetrolType(carDetails.getPetrolType() != null ? new LookupEntity(carDetails.getPetrolType()) : null);
        particularities.setGearsType(carDetails.getGearsType() != null ? new LookupEntity(carDetails.getGearsType()) : null);
        particularities.setTractionType(carDetails.getTractionType() != null ? new LookupEntity(carDetails.getTractionType()) : null);
        particularities.setColor(carDetails.getColor() != null ? new LookupEntity(carDetails.getColor()) : null);

        return particularities;
    }

    @Override
    public void save(CarDetails carDetails, Connection conn) throws SQLException {
        Particularities particularities = map(carDetails);
        String insertSql = """
            INSERT INTO particularities (
                author, year_of_fabrication, wheel_side_id, nr_of_seats_id, body_id, nr_of_doors_id,
                engine_capacity_id, horsepower_id, petrol_type_id,
                gears_type_id, traction_type_id, color_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
            """;
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            prepareParticularitiesStatement(particularities, stmt, conn);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                particularities.setId(rs.getInt("id"));
            }
        }
    }

    @Override
    public void saveBatch(List<CarDetails> carDetailsList, Connection conn) throws SQLException {
        String insertSql = """
            INSERT INTO particularities (
                author, year_of_fabrication, wheel_side_id, nr_of_seats_id, body_id, nr_of_doors_id,
                engine_capacity_id, horsepower_id, petrol_type_id,
                gears_type_id, traction_type_id, color_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
            """;
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            for (CarDetails carDetails : carDetailsList) {
                Particularities particularities = map(carDetails);
                prepareParticularitiesStatement(particularities, stmt, conn);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public long saveAndReturnId(CarDetails carDetails, Connection conn) throws SQLException {
        Particularities particularities = map(carDetails);
        String insertSql = """
            INSERT INTO particularities (
                author, year_of_fabrication, wheel_side_id, nr_of_seats_id, body_id, nr_of_doors_id,
                engine_capacity_id, horsepower_id, petrol_type_id,
                gears_type_id, traction_type_id, color_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
            """;
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            prepareParticularitiesStatement(particularities, stmt, conn);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                particularities.setId(rs.getInt("id"));
                return particularities.getId();
            }
        }
        return -1;
    }
}