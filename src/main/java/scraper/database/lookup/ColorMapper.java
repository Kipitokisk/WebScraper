package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.Color;

public class ColorMapper extends LookupEntityMapper<Color> {
    public ColorMapper(DatabaseManager dbManager) {
        super("color", dbManager, Color::new);
    }
}
