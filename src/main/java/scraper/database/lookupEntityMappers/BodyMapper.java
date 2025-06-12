package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.Body;

public class BodyMapper extends LookupEntityMapper<Body> {
    public BodyMapper(DatabaseManager dbManager) {
        super("body", dbManager, Body::new);
    }
}