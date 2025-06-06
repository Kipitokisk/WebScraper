package scraper.logic;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DatabaseManagerHelperTest {
    private final DatabaseManagerHelper databaseManagerHelper = new DatabaseManagerHelper();

    @Test
    void testParseRomanianDate_WithValidDate() {
        String romanianDate = "15 ian. 2024, 14:30";
        Timestamp result = databaseManagerHelper.parseRomanianDate(romanianDate);

        assertNotNull(result);

        // Verify the parsed date
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);
        String expectedNormalized = "15 Jan 2024, 14:30";
        try {
            java.util.Date utilDate = sdf.parse(expectedNormalized);
            java.sql.Date expected = new java.sql.Date(utilDate.getTime());
            assertEquals(expected, result);
        } catch (ParseException e) {
            fail("Failed to parse expected date for comparison");
        }
    }

    @Test
    void testParseRomanianDate_WithAllMonths() {
        String[] romanianMonths = {"ian.", "feb.", "mar.", "apr.", "mai.", "iun.",
                "iul.", "aug.", "sept.", "oct.", "nov.", "dec."};
        String[] englishMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int i = 0; i < romanianMonths.length; i++) {
            String romanianDate = "15 " + romanianMonths[i] + " 2024, 10:00";
            Timestamp result = databaseManagerHelper.parseRomanianDate(romanianDate);

            assertNotNull(result, "Failed to parse date with month: " + romanianMonths[i]);

            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);
            String expectedDate = "15 " + englishMonths[i] + " 2024, 10:00";
            try {
                java.util.Date utilDate = sdf.parse(expectedDate);
                java.sql.Date expected = new java.sql.Date(utilDate.getTime());
                assertEquals(expected, result);
            } catch (ParseException e) {
                fail("Failed to parse expected date for month: " + englishMonths[i]);
            }
        }
    }

    @Test
    void testParseRomanianDate_WithNullValue() {
        Timestamp result = databaseManagerHelper.parseRomanianDate(null);
        assertNull(result);
    }

    @Test
    void testParseRomanianDate_WithBlankValue() {
        Timestamp result = databaseManagerHelper.parseRomanianDate("   ");
        assertNull(result);
    }

    @Test
    void testParseRomanianDate_WithEmptyString() {
        Timestamp result = databaseManagerHelper.parseRomanianDate("");
        assertNull(result);
    }

    @Test
    void testParseRomanianDate_WithInvalidFormat() {
        String invalidDate = "invalid date format";
        Timestamp result = databaseManagerHelper.parseRomanianDate(invalidDate);
        assertNull(result);
    }
}
