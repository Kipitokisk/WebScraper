package scraper.database;

import scraper.model.GearsType;

public class GearsTypeMapper extends LookupEntityMapper<GearsType> {
    public GearsTypeMapper(DatabaseManager dbManager) {
        super("gears_type", dbManager, GearsType::new);
    }
}
