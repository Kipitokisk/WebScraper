package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.Color;

public class ColorMapper extends LookupEntityMapper<Color> {
    public ColorMapper(DatabaseManager dbManager) {
        super("color", dbManager, Color::new);
    }
}
