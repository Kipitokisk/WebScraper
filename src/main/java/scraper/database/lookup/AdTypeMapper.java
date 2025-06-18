package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.AdType;

public class AdTypeMapper extends LookupEntityMapper<AdType> {
    public AdTypeMapper(DatabaseManager dbManager) {
        super("ad_type", dbManager, AdType::new);
    }
}
