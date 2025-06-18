package scraper.database.lookup;

import scraper.database.DatabaseManager;
import scraper.model.lookup.NrOfSeats;

public class NrOfSeatsMapper extends LookupEntityMapper<NrOfSeats> {
    public NrOfSeatsMapper(DatabaseManager dbManager) {
        super("nr_of_seats", dbManager, NrOfSeats::new);
    }
}