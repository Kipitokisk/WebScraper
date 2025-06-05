package scraper.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        String input = "12 Ian. 2024";
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

}