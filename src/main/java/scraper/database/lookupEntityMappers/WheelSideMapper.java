package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.WheelSide;

public class WheelSideMapper extends LookupEntityMapper<WheelSide> {
    public WheelSideMapper(DatabaseManager dbManager) {
        super("wheel_side", dbManager, WheelSide::new);
    }
}