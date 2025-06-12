package scraper.database.lookupEntityMappers;

import scraper.database.DatabaseManager;
import scraper.model.lookupEntity.NrOfSeats;

public class NrOfSeatsMapper extends LookupEntityMapper<NrOfSeats> {
    public NrOfSeatsMapper(DatabaseManager dbManager) {
        super("nr_of_seats", dbManager, NrOfSeats::new);
    }
}