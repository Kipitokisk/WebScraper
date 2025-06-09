package scraper.logic;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseManagerHelper {
    public Timestamp parseRomanianDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;

        String replaced = replaceDate(dateStr);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);
            Date parsed = sdf.parse(replaced);
            return new Timestamp(parsed.getTime());
        } catch (ParseException e) {
            System.err.println("Failed to parse normalized date: " + replaced);
            return null;
        }
    }

    String replaceDate(String dateStr) {
        String replaced = dateStr
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
        return replaced;
    }
}
