package scraper.database;

import scraper.model.Color;

public class ColorMapper extends LookupEntityMapper<Color>{
    public ColorMapper(DatabaseManager dbManager) {
        super("color", dbManager, Color::new);
    }
}
