package scraper.database;

import scraper.model.CarDetails;
import scraper.model.LookupEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static scraper.database.DatabaseUtils.setNullableString;

public class LookupEntityMapper implements EntityMapper<LookupEntity> {
    private static final String SELECT_SQL = "SELECT id FROM %s WHERE \"name\" = ?";
    private static final String INSERT_SQL = "INSERT INTO %s (\"name\") VALUES (?) ON CONFLICT (\"name\") DO NOTHING RETURNING id";

    private final String tableName;
    private final DatabaseManager dbManager;

    public LookupEntityMapper(String tableName, DatabaseManager dbManager) {
        this.tableName = tableName;
        this.dbManager = dbManager;
    }

    Integer getOrInsertLookup(String value) throws SQLException {
        if (value == null) return null;

        try (Connection conn = dbManager.getConnection()) {
            String selectSql = String.format(SELECT_SQL, tableName);
            try (PreparedStatement stmt = dbManager.prepareStatement(conn, selectSql)) {
                setNullableString(stmt, 1, value);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }

            String insertSql = String.format(INSERT_SQL, tableName);
            try (PreparedStatement stmt = dbManager.prepareStatement(conn, insertSql)) {
                setNullableString(stmt, 1, value);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
        }

        return null;
    }

    @Override
    public LookupEntity map(CarDetails carDetails) {
        return new LookupEntity(null);
    }

    @Override
    public void save(CarDetails carDetails) throws SQLException {
        LookupEntity entity = map(carDetails);
        String value = carDetails.getAdType() != null ? carDetails.getAdType() : null;
        Integer id = getOrInsertLookup(value);
        entity.setId(id);
    }

    @Override
    public void saveBatch(List<CarDetails> carDetailsList) throws SQLException {
        for (CarDetails carDetails : carDetailsList) {
            LookupEntity entity = map(carDetails);
            String value = carDetails.getAdType() != null ? carDetails.getAdType() : null;
            Integer id = getOrInsertLookup(value);
            entity.setId(id);
        }
    }
}