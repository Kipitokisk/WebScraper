package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.WheelSide;

public class WheelSideMapper extends LookupEntityMapper<WheelSide> {
    public WheelSideMapper(DatabaseManager dbManager) {
        super("wheel_side", dbManager, WheelSide::new);
    }
}