package scraper.database;

import scraper.model.AdType;

public class AdTypeMapper extends LookupEntityMapper<AdType> {
    public AdTypeMapper(DatabaseManager dbManager) {
        super("ad_type", dbManager, AdType::new);
    }
}
