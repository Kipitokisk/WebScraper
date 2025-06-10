package scraper.database;

import scraper.model.EngineCapacity;

public class EngineCapacityMapper extends LookupEntityMapper<EngineCapacity> {
    public EngineCapacityMapper(DatabaseManager dbManager) {
        super("engine_capacity", dbManager, EngineCapacity::new);
    }
}
