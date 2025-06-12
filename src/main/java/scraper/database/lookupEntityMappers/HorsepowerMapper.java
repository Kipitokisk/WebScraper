package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.Horsepower;

public class HorsepowerMapper extends LookupEntityMapper<Horsepower> {
    public HorsepowerMapper(DatabaseManager dbManager) {
        super("horsepower", dbManager, Horsepower::new);
    }
}
