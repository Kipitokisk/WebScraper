package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.AdType;

public class AdTypeMapper extends LookupEntityMapper<AdType> {
    public AdTypeMapper(DatabaseManager dbManager) {
        super("ad_type", dbManager, AdType::new);
    }
}
