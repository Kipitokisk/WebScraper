package scraper.database;

import scraper.model.CarDetails;
import scraper.model.LookupEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class LookupEntityMapper implements EntityMapper<LookupEntity> {
    private final String tableName;
    private final String value;

    public LookupEntityMapper(String tableName, String value) {
        this.tableName = tableName;
        this.value = value;
    }

    public Integer getOrInsertLookup(Connection conn) throws SQLException {
        if (value == null) return null;

        String selectSql = "SELECT id FROM " + tableName + " WHERE \"name\" = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, value);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }

        String insertSql = "INSERT INTO " + tableName + " (\"name\") VALUES (?) RETURNING id";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, value);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }

        return null;
    }

    @Override
    public LookupEntity map(CarDetails carDetails) {
        return new LookupEntity(value);
    }

    @Override
    public void save(CarDetails carDetails, Connection conn) throws SQLException {
        LookupEntity entity = map(carDetails);
        Integer id = getOrInsertLookup(conn);
        entity.setId(id);
    }

    @Override
    public void saveBatch(List<CarDetails> carDetailsList, Connection conn) throws SQLException {
        for (CarDetails carDetails : carDetailsList) {
            save(carDetails, conn);
        }
    }
}