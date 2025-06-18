package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.EngineCapacity;

public class EngineCapacityMapper extends LookupEntityMapper<EngineCapacity> {
    public EngineCapacityMapper(DatabaseManager dbManager) {
        super("engine_capacity", dbManager, EngineCapacity::new);
    }
}
