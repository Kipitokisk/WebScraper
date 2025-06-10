package scraper.database;

import scraper.model.TractionType;

public class TractionTypeMapper extends LookupEntityMapper<TractionType>{
    public TractionTypeMapper(DatabaseManager dbManager) {
        super("traction_type", dbManager, TractionType::new);
    }
}
