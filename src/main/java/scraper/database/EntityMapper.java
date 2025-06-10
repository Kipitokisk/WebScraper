package scraper.database;

import scraper.model.CarDetails;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

interface EntityMapper<T> {
    T map(CarDetails carDetails);
    void save(CarDetails carDetails) throws SQLException;
    void saveBatch(List<CarDetails> carDetailsList) throws SQLException;
}