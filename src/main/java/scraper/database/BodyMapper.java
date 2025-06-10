package scraper.database;

import scraper.model.Body;

public class BodyMapper extends LookupEntityMapper<Body> {
    public BodyMapper(DatabaseManager dbManager) {
        super("body", dbManager, Body::new);
    }
}