package scraper.database;

import scraper.model.NrOfDoors;

public class NrOfDoorsMapper extends LookupEntityMapper<NrOfDoors> {
    public NrOfDoorsMapper(DatabaseManager dbManager) {
        super("nr_of_doors", dbManager, NrOfDoors::new);
    }
}