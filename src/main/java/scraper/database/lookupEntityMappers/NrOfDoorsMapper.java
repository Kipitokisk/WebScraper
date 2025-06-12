package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.NrOfDoors;

public class NrOfDoorsMapper extends LookupEntityMapper<NrOfDoors> {
    public NrOfDoorsMapper(DatabaseManager dbManager) {
        super("nr_of_doors", dbManager, NrOfDoors::new);
    }
}