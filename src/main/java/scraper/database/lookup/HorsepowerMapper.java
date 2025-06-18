package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.Horsepower;

public class HorsepowerMapper extends LookupEntityMapper<Horsepower> {
    public HorsepowerMapper(DatabaseManager dbManager) {
        super("horsepower", dbManager, Horsepower::new);
    }
}
