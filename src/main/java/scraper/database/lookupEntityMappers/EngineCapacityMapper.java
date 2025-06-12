package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.EngineCapacity;

public class EngineCapacityMapper extends LookupEntityMapper<EngineCapacity> {
    public EngineCapacityMapper(DatabaseManager dbManager) {
        super("engine_capacity", dbManager, EngineCapacity::new);
    }
}
