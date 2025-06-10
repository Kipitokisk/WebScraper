package scraper.database;

import scraper.model.PetrolType;

public class PetrolTypeMapper extends LookupEntityMapper<PetrolType>{
    public PetrolTypeMapper(DatabaseManager dbManager) {
        super("petrol_type", dbManager, PetrolType::new);
    }
}
