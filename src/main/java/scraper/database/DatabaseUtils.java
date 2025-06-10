package scraper.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseUtils {
    public static void setNullableString(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.VARCHAR);
        } else {
            stmt.setString(index, value);
        }
    }

    public static void setNullableInt(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }

    public static Timestamp parseRomanianDate(String dateStr) {
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

    public static String replaceDate(String dateStr) {
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