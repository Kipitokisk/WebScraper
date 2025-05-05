import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/scraper_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "pass";

    public void saveCars(Map<String, List<Integer>> finalProducts) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO cars (link, price, mileage) VALUES (?, ?, ?) ON CONFLICT (link) DO NOTHING";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map.Entry<String, List<Integer>> entry : finalProducts.entrySet()) {
                    String link = entry.getKey();
                    List<Integer> values = entry.getValue();
                    if (values == null || values.size() < 2) continue;

                    if (link.contains("?clickToken")) {
                        int queryIndex = link.indexOf('?');
                        if (queryIndex != -1) {
                            link = link.substring(0, queryIndex);
                        }
                    }

                    stmt.setString(1, link);
                    stmt.setInt(2, values.get(0));
                    stmt.setInt(3, values.get(1));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
    }
}