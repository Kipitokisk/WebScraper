package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.PetrolType;

public class PetrolTypeMapper extends LookupEntityMapper<PetrolType> {
    public PetrolTypeMapper(DatabaseManager dbManager) {
        super("petrol_type", dbManager, PetrolType::new);
    }
}
