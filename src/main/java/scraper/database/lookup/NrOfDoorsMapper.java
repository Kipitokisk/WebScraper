package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.NrOfDoors;

public class NrOfDoorsMapper extends LookupEntityMapper<NrOfDoors> {
    public NrOfDoorsMapper(DatabaseManager dbManager) {
        super("nr_of_doors", dbManager, NrOfDoors::new);
    }
}