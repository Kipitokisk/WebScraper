package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.TractionType;

public class TractionTypeMapper extends LookupEntityMapper<TractionType> {
    public TractionTypeMapper(DatabaseManager dbManager) {
        super("traction_type", dbManager, TractionType::new);
    }
}
