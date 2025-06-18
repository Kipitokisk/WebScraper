package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.TractionType;

public class TractionTypeMapper extends LookupEntityMapper<TractionType> {
    public TractionTypeMapper(DatabaseManager dbManager) {
        super("traction_type", dbManager, TractionType::new);
    }
}
