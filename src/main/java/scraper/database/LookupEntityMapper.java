package scraper.database;

import scraper.model.LookupEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public abstract class LookupEntityMapper<T extends LookupEntity> {
    protected final String tableName;
    protected final DatabaseManager dbManager;
    private final Function<String, T> entityConstructor;

    protected static final String INSERT_SQL = "INSERT INTO %s (name) VALUES (?) ON CONFLICT (name) DO NOTHING RETURNING id";
    protected static final String SELECT_SQL = "SELECT id, name FROM %s";

    protected LookupEntityMapper(String tableName, DatabaseManager dbManager, Function<String, T> entityConstructor) {
        this.tableName = tableName;
        this.dbManager = dbManager;
        this.entityConstructor = entityConstructor;
    }

    public Integer getOrInsertLookup(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, String.format(INSERT_SQL, tableName))) {
            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
            // If no ID was returned (due to conflict), fetch existing ID
            try (PreparedStatement selectStmt = dbManager.prepareStatement(conn,
                    String.format("SELECT id FROM %s WHERE name = ?", tableName))) {
                selectStmt.setString(1, name.trim());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    return rs.next() ? rs.getInt("id") : null;
                }
            }
        }
    }

    public void batchInsert(Set<String> names) throws SQLException {
        if (names == null || names.isEmpty()) {
            return;
        }
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = dbManager.prepareStatement(conn, String.format(INSERT_SQL, tableName))) {
            for (String name : names) {
                if (name != null && !name.trim().isEmpty()) {
                    stmt.setString(1, name.trim());
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    public List<T> getAll() throws SQLException {
        List<T> entities = new ArrayList<>();
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