package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.Body;

public class BodyMapper extends LookupEntityMapper<Body> {
    public BodyMapper(DatabaseManager dbManager) {
        super("body", dbManager, Body::new);
    }
}