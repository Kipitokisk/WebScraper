package scraper.database;

import scraper.model.Horsepower;

public class HorsepowerMapper extends LookupEntityMapper<Horsepower> {
    public HorsepowerMapper(DatabaseManager dbManager) {
        super("horsepower", dbManager, Horsepower::new);
    }
}
