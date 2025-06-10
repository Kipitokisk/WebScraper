package scraper.database;

import scraper.model.NrOfSeats;

public class NrOfSeatsMapper extends LookupEntityMapper<NrOfSeats> {
    public NrOfSeatsMapper(DatabaseManager dbManager) {
        super("nr_of_seats", dbManager, NrOfSeats::new);
    }
}