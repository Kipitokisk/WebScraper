package scraper.logic;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabaseManagerHelperTest {
    private final DatabaseManagerHelper dbHelper = new DatabaseManagerHelper();

    @Test
    void testReplaceDate_WithAllMonths_Success() {
        String[] romanianMonths = {"ian.", "feb.", "mar.", "apr.", "mai.", "iun.",
                "iul.", "aug.", "sept.", "oct.", "nov.", "dec."};
        String[] englishMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int i = 0; i < romanianMonths.length; i++) {
            String romanianDate = romanianMonths[i];
            String englishDate = dbHelper.replaceDate(romanianDate);
            assertEquals(englishMonths[i], englishDate);
        }
    }

    @Test
    void testReplaceDate_WithFullDateString() {
        String input = "12 ian. 2024";
        String expected = "12 Jan 2024";
        assertEquals(expected, dbHelper.replaceDate(input));
    }

    @Test
    void testReplaceDate_WithNoMonth_ShouldRemainUnchanged() {
        String input = "Hello world!";
        assertEquals("Hello world!", dbHelper.replaceDate(input));
    }

    @Test
    void testReplaceDate_CaseSensitiveMismatch() {
        String input = "12 Ian. 2024";  // "Ian." instead of "ian."
        assertEquals("12 Ian. 2024", dbHelper.replaceDate(input));
    }

    @Test
    void testReplaceDate_EmptyString() {
        assertEquals("", dbHelper.replaceDate(""));
    }

    @Test
    void testReplaceDate_MultipleMonthsInString() {
        String input = "ian. to feb. to mar.";
        String expected = "Jan to Feb to Mar";
        assertEquals(expected, dbHelper.replaceDate(input));
    }

    @Test
    void testParseRomanianDate_WithAllMonths() {
        String[] romanianMonths = {"ian.", "feb.", "mar.", "apr.", "mai.", "iun.",
                "iul.", "aug.", "sept.", "oct.", "nov.", "dec."};
        String[] englishMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int i = 0; i < romanianMonths.length; i++) {
            String romanianDate = "15 " + romanianMonths[i] + " 2024, 10:00";
            Timestamp result = dbHelper.parseRomanianDate(romanianDate);

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
        Timestamp result = dbHelper.parseRomanianDate(null);
        assertNull(result);
    }

    @Test
    void testParseRomanianDate_WithBlankValue() {
        Timestamp result = dbHelper.parseRomanianDate("   ");
        assertNull(result);
    }

    @Test
    void testParseRomanianDate_WithEmptyString() {
        Timestamp result = dbHelper.parseRomanianDate("");
        assertNull(result);
    }

    @Test
    void testParseRomanianDate_WithInvalidFormat() {
        String invalidDate = "invalid date format";
        Timestamp result = dbHelper.parseRomanianDate(invalidDate);
        assertNull(result);
    }
}
