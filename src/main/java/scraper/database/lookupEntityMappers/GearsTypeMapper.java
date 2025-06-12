package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.GearsType;

public class GearsTypeMapper extends LookupEntityMapper<GearsType> {
    public GearsTypeMapper(DatabaseManager dbManager) {
        super("gears_type", dbManager, GearsType::new);
    }
}
