package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.PetrolType;

public class PetrolTypeMapper extends LookupEntityMapper<PetrolType> {
    public PetrolTypeMapper(DatabaseManager dbManager) {
        super("petrol_type", dbManager, PetrolType::new);
    }
}
