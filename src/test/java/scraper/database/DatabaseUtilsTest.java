//package scraper.database;
//
//import org.junit.jupiter.api.Test;
//
//import java.sql.PreparedStatement;
//import java.sql.SQLException;
//import java.sql.Timestamp;
//import java.sql.Types;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Locale;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//import static scraper.database.DatabaseUtils.parseRomanianDate;
//import static scraper.database.DatabaseUtils.replaceDate;
//
//class DatabaseUtilsTest {
//
//    @Test
//    void testSetNullableString_WithNullValue() throws SQLException {
//        PreparedStatement stmt = mock(PreparedStatement.class);
//        DatabaseUtils.setNullableString(stmt, 1, null);
//        verify(stmt).setNull(1, Types.VARCHAR);
//        verifyNoMoreInteractions(stmt);
//    }
//
//    @Test
//    void testSetNullableString_WithValidValue() throws SQLException {
//        PreparedStatement stmt = mock(PreparedStatement.class);
//        String value = "Test";
//        DatabaseUtils.setNullableString(stmt, 1, value);
//        verify(stmt).setString(1, value);
//        verifyNoMoreInteractions(stmt);
//    }
//
//    @Test
//    void testSetNullableString_WithEmptyString() throws SQLException {
//        PreparedStatement stmt = mock(PreparedStatement.class);
//        String value = "";
//        DatabaseUtils.setNullableString(stmt, 1, value);
//        verify(stmt).setString(1, value);
//        verifyNoMoreInteractions(stmt);
//    }
//
//    @Test
//    void testSetNullableInt_WithNullValue() throws SQLException {
//        PreparedStatement stmt = mock(PreparedStatement.class);
//        DatabaseUtils.setNullableInt(stmt, 1, null);
//        verify(stmt).setNull(1, Types.INTEGER);
//        verifyNoMoreInteractions(stmt);
//    }
//
//    @Test
//    void testSetNullableInt_WithValidValue() throws SQLException {
//        PreparedStatement stmt = mock(PreparedStatement.class);
//        Integer value = 2024;
//        DatabaseUtils.setNullableInt(stmt, 1, value);
//        verify(stmt).setInt(1, value);
//        verifyNoMoreInteractions(stmt);
//    }
//
//    @Test
//    void testSetNullableInt_WithZeroValue() throws SQLException {
//        PreparedStatement stmt = mock(PreparedStatement.class);
//        Integer value = 0;
//        DatabaseUtils.setNullableInt(stmt, 1, value);
//        verify(stmt).setInt(1, value);
//        verifyNoMoreInteractions(stmt);
//    }
//
//    @Test
//    void testReplaceDate_WithAllMonths_Success() {
//        String[] romanianMonths = {"ian.", "feb.", "mar.", "apr.", "mai.", "iun.",
//                "iul.", "aug.", "sept.", "oct.", "nov.", "dec."};
//        String[] englishMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
//                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
//
//        for (int i = 0; i < romanianMonths.length; i++) {
//            String romanianDate = romanianMonths[i];
//            String englishDate = replaceDate(romanianDate);
//            assertEquals(englishMonths[i], englishDate);
//        }
//    }
//
//    @Test
//    void testReplaceDate_WithFullDateString() {
//        String input = "12 ian. 2024";
//        String expected = "12 Jan 2024";
//        assertEquals(expected, replaceDate(input));
//    }
//
//    @Test
//    void testReplaceDate_WithNoMonth_ShouldRemainUnchanged() {
//        String input = "Hello world!";
//        assertEquals("Hello world!", replaceDate(input));
//    }
//
//    @Test
//    void testReplaceDate_CaseSensitiveMismatch() {
//        String input = "12 Ian. 2024";
//        assertEquals("12 Ian. 2024", replaceDate(input));
//    }
//
//    @Test
//    void testReplaceDate_EmptyString() {
//        assertEquals("", replaceDate(""));
//    }
//
//    @Test
//    void testReplaceDate_MultipleMonthsInString() {
//        String input = "ian. to feb. to mar.";
//        String expected = "Jan to Feb to Mar";
//        assertEquals(expected, replaceDate(input));
//    }
//
//    @Test
//    void testParseRomanianDate_WithAllMonths() {
//        String[] romanianMonths = {"ian.", "feb.", "mar.", "apr.", "mai.", "iun.",
//                "iul.", "aug.", "sept.", "oct.", "nov.", "dec."};
//        String[] englishMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
//                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
//
//        for (int i = 0; i < romanianMonths.length; i++) {
//            String romanianDate = "15 " + romanianMonths[i] + " 2024, 10:00";
//            Timestamp result = parseRomanianDate(romanianDate);
//
//            assertNotNull(result, "Failed to parse date with month: " + romanianMonths[i]);
//
//            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);
//            String expectedDate = "15 " + englishMonths[i] + " 2024, 10:00";
//            try {
//                java.util.Date utilDate = sdf.parse(expectedDate);
//                java.sql.Date expected = new java.sql.Date(utilDate.getTime());
//                assertEquals(expected, result);
//            } catch (ParseException e) {
//                fail("Failed to parse expected date for month: " + englishMonths[i]);
//            }
//        }
//    }
//
//    @Test
//    void testParseRomanianDate_WithNullValue() {
//        Timestamp result = parseRomanianDate(null);
//        assertNull(result);
//    }
//
//    @Test
//    void testParseRomanianDate_WithBlankValue() {
//        Timestamp result = parseRomanianDate("   ");
//        assertNull(result);
//    }
//
//    @Test
//    void testParseRomanianDate_WithEmptyString() {
//        Timestamp result = parseRomanianDate("");
//        assertNull(result);
//    }
//
//    @Test
//    void testParseRomanianDate_WithInvalidFormat() {
//        String invalidDate = "invalid date format";
//        Timestamp result = parseRomanianDate(invalidDate);
//        assertNull(result);
//    }
//}
