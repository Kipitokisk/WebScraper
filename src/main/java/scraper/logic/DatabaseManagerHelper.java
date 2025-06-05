package scraper.logic;

public class DatabaseManagerHelper {
    public String replaceDate(String dateStr) {
        return dateStr
                .replace("ian.", "Jan")
                .replace("feb.", "Feb")
                .replace("mar.", "Mar")
                .replace("apr.", "Apr")
                .replace("mai.", "May")
                .replace("iun.", "Jun")
                .replace("iul.", "Jul")
                .replace("aug.", "Aug")
                .replace("sept.", "Sep")
                .replace("oct.", "Oct")
                .replace("nov.", "Nov")
                .replace("dec.", "Dec");
    }
}