package scraper.database;

import scraper.database.registry.LookupEntityRegistry;
import scraper.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static scraper.database.DatabaseUtils.*;

public class ParticularitiesMapper implements EntityMapper<Particularities> {
    private static final String INSERT_SQL = """
        INSERT INTO particularities (
            author, link, year_of_fabrication, wheel_side_id, nr_of_seats_id, body_id,
            nr_of_doors_id, engine_capacity_id, horsepower_id, petrol_type_id,
            gears_type_id, traction_type_id, color_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (link) DO NOTHING
        RETURNING id
        """;
    private static final String SELECT_SQL = """
        SELECT id, author, link, year_of_fabrication, wheel_side_id, nr_of_seats_id, body_id,
               nr_of_doors_id, engine_capacity_id, horsepower_id, petrol_type_id,
               gears_type_id, traction_type_id, color_id
        FROM particularities
        """;


    private final DatabaseManager dbManager;
    private final LookupEntityRegistry lookupRegistry;

    public ParticularitiesMapper(DatabaseManager dbManager, LookupEntityRegistry lookupRegistry) {
        this.dbManager = dbManager;
        this.lookupRegistry = lookupRegistry;
    }

    private void prepareParticularitiesStatement(Particularities particularities, PreparedStatement stmt) throws SQLException {
        Integer wheelSideId = particularities.getWheelSide();
        Integer nrOfSeatsId = particularities.getNrOfSeats();
        Integer bodyId = particularities.getBody();
        Integer nrOfDoorsId = particularities.getNrOfDoors();
        Integer engineCapacityId = particularities.getEngineCapacity();
        Integer horsepowerId = particularities.getHorsepower();
        Integer petrolTypeId = particularities.getPetrolType();
        Integer gearsTypeId = particularities.getGearsType();
        Integer tractionTypeId = particularities.getTractionType();
        Integer colorId = particularities.getColor();

        setNullableString(stmt, 1, particularities.getAuthor());
        setNullableString(stmt, 2, particularities.getLink());
        setNullableInt(stmt, 3, particularities.getYearOfFabrication());
        setNullableInt(stmt, 4, wheelSideId);
        setNullableInt(stmt, 5, nrOfSeatsId);
        setNullableInt(stmt, 6, bodyId);
        setNullableInt(stmt, 7, nrOfDoorsId);
        setNullableInt(stmt, 8, engineCapacityId);
        setNullableInt(stmt, 9, horsepowerId);
        setNullableInt(stmt, 10, petrolTypeId);
        setNullableInt(stmt, 11, gearsTypeId);
        setNullableInt(stmt, 12, tractionTypeId);
        setNullableInt(stmt, 13, colorId);
    }

    @Override
    public Particularities map(CarDetails carDetails) {
        return new Particularities.Builder()
                .author(carDetails.getAuthor())
                .link(carDetails.getLink())
                .yearOfFabrication(carDetails.getYearOfFabrication())
                .wheelSide(lookupRegistry.getWheelSideId(carDetails.getWheelSide()))
                .nrOfSeats(lookupRegistry.getNrOfSeatsId(carDetails.getNrOfSeats()))
                .body(lookupRegistry.getBodyId(carDetails.getBody()))
                .nrOfDoors(lookupRegistry.getNrOfDoorsId(carDetails.getNrOfDoors()))
                .engineCapacity(lookupRegistry.getEngineCapacityId(carDetails.getEngineCapacity()))
                .horsepower(lookupRegistry.getHorsepowerId(carDetails.getHorsepower()))
                .petrolType(lookupRegistry.getPetrolTypeId(carDetails.getPetrolType()))
                .gearsType(lookupRegistry.getGearsTypeId(carDetails.getGearsType()))
                .tractionType(lookupRegistry.getTractionTypeId(carDetails.getTractionType()))
                .color(lookupRegistry.getColorId(carDetails.getColor()))
                .build();
    }

    @Override
    public void saveBatch(List<CarDetails> carDetailsList) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, INSERT_SQL)) {
            for (CarDetails carDetails : carDetailsList) {
                Particularities particularities = map(carDetails);
                prepareParticularitiesStatement(particularities, stmt);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public List<Particularities> getAll() throws SQLException {
        List<Particularities> particularitiesList = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, SELECT_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Particularities particularities = new Particularities.Builder()
                        .id(rs.getInt("id"))
                        .link(rs.getString("link"))
                        .author(rs.getString("author"))
                        .yearOfFabrication(rs.getInt("year_of_fabrication"))
                        .wheelSide(rs.getInt("wheel_side_id"))
                        .nrOfSeats(rs.getInt("nr_of_seats_id"))
                        .body(rs.getInt("body_id"))
                        .nrOfDoors(rs.getInt("nr_of_doors_id"))
                        .engineCapacity(rs.getInt("engine_capacity_id"))
                        .horsepower(rs.getInt("horsepower_id"))
                        .petrolType(rs.getInt("petrol_type_id"))
                        .gearsType(rs.getInt("gears_type_id"))
                        .tractionType(rs.getInt("traction_type_id"))
                        .color(rs.getInt("color_id"))
                        .build();

                particularitiesList.add(particularities);
            }
        }

        return particularitiesList;
    }
}