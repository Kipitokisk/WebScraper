package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.LookupEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public abstract class LookupEntityMapper<T extends LookupEntity> {
    protected final String tableName;
    protected final DatabaseManager dbManager;
    private final Function<String, T> entityConstructor;

    protected static final String INSERT_SQL = "INSERT INTO %s (name) VALUES (?) ON CONFLICT (name) DO NOTHING";
    protected static final String SELECT_SQL = "SELECT id, name FROM %s";

    protected LookupEntityMapper(String tableName, DatabaseManager dbManager, Function<String, T> entityConstructor) {
        this.tableName = tableName;
        this.dbManager = dbManager;
        this.entityConstructor = entityConstructor;
    }

    public void saveBatch(Set<T> entities) throws SQLException {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, String.format(INSERT_SQL, tableName))) {
            for (LookupEntity entity : entities) {
                if (entity.getName() != null && !entity.getName().trim().isEmpty()) {
                    stmt.setString(1, entity.getName().trim());
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    public Set<T> getAll() throws SQLException {
        Set<T> entities = new HashSet<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, String.format(SELECT_SQL, tableName));
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                T entity = entityConstructor.apply(rs.getString("name"));
                entity.setId(rs.getInt("id"));
                entities.add(entity);
            }
        }
        return entities;
    }
}