package scraper.database;

import scraper.model.WheelSide;

public class WheelSideMapper extends LookupEntityMapper<WheelSide> {
    public WheelSideMapper(DatabaseManager dbManager) {
        super("wheel_side", dbManager, WheelSide::new);
    }
}