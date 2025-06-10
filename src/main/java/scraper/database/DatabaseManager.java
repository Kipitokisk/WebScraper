package scraper.database;

import scraper.model.CarDetails;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DatabaseManager {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final CarsMapper carsMapper = new CarsMapper();

    public DatabaseManager(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public void saveCars(List<CarDetails> finalProducts) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false);
            try {
                carsMapper.saveBatch(finalProducts, conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}