package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.GearsType;

public class GearsTypeMapper extends LookupEntityMapper<GearsType> {
    public GearsTypeMapper(DatabaseManager dbManager) {
        super("gears_type", dbManager, GearsType::new);
    }
}
