package scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScraperTest {
    @InjectMocks
    private Scraper scraper;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        scraper = new Scraper("https://999.md", "https://999.md/ro/list/transport/cars?appl=1&ef=16,1,6,2200&eo=12885,12900,12912,139,35538&aof=20&o_1_2095_8_98=36188");
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void testExtractDetailedCarInfo_Success() throws IOException {
        String carLink = "/car";
        String html = "<h1>Renault Megane</h1>" +
                "<div class=\"styles_aside__0m8KW\">" +
                "<p class=\"styles_date__voWnk\">Data actualizării: 2023-10-01</p>" +
                "<p class=\"styles_type___J9Dy\">Tip: Vând</p>" +
                "<span class=\"styles_sidebar__main__DaXQC\">15000 €</span>" +
                "<span class=\"styles_address__text__duvKg\">Orhei</span>" +
                "<a class=\"styles_owner__login__VKE71\">John</a>" +
                "</div>" +
                "<div class=\"styles_features__left__ON_QP\">" +
                "<div class=\"styles_group__aota8\">" +
                "<ul>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Generație</span>" +
                "<span class=\"styles_group__value__XN7OI\">III (2008 - 2016)</span></li>" +
                "</ul></div>" +
                "<div class=\"styles_features__right__Sn6fV\">" +
                "<div class=\"styles_group__aota8\">" +
                "<ul>" +
                "<li><span class=\"styles_group__key__uRhnQ\">An de fabricație</span>" +
                "<span class=\"styles_group__value__XN7OI\">2016</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Volan</span>" +
                "<span class=\"styles_group__value__XN7OI\">Stânga</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Număr de locuri</span>" +
                "<span class=\"styles_group__value__XN7OI\">5</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Tip caroserie</span>" +
                "<span class=\"styles_group__value__XN7OI\">Sedan</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Număr uși</span>" +
                "<span class=\"styles_group__value__XN7OI\">4</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Capacitate cilindrică</span>" +
                "<span class=\"styles_group__value__XN7OI\">1800 cm³</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Putere</span>" +
                "<span class=\"styles_group__value__XN7OI\">132 CP</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Tip combustibil</span>" +
                "<span class=\"styles_group__value__XN7OI\">Benzină</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Cutie de viteze</span>" +
                "<span class=\"styles_group__value__XN7OI\">Automat</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Tip tracțiune</span>" +
                "<span class=\"styles_group__value__XN7OI\">Față</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Culoare</span>" +
                "<span class=\"styles_group__value__XN7OI\">Alb</span></li>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Rulaj</span>" +
                "<span class=\"styles_group__value__XN7OI\">100000 km</span></li>" +
                "</ul></div></div>";
        Document doc = Jsoup.parse(html);

        try (var jsoupMock = mockStatic(Jsoup.class)) {
            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mock(org.jsoup.Connection.class));
            when(Jsoup.connect(anyString()).get()).thenReturn(doc);

            CarDetails result = scraper.extractDetailedCarInfo(carLink);

            assertNotNull(result, "scraper.CarDetails should not be null");
            assertEquals("https://999.md/car", result.getLink(), "Link should match");
            assertEquals("Renault Megane III (2008 - 2016)", result.getName(), "Name should match");
            assertEquals(15000, result.getEurPrice(), "Price should match");
            assertEquals(100000, result.getMileage(), "Mileage should match");
            assertEquals("2023-10-01", result.getUpdateDate(), "Update date should match");
            assertEquals("Vând", result.getAdType(), "Ad type should match");
            assertEquals("Orhei", result.getRegion(), "Region should match");
            assertEquals("John", result.getAuthor(), "Author should match");
            assertEquals(2016, result.getYearOfFabrication(), "Year should match");
            assertEquals("Stânga", result.getWheelSide(), "Wheel side should match");
            assertEquals(5, result.getNrOfSeats(), "Number of seats should match");
            assertEquals("Sedan", result.getBody(), "Body should match");
            assertEquals(4, result.getNrOfDoors(), "Number of doors should match");
            assertEquals(1800, result.getEngineCapacity(), "Engine capacity should match");
            assertEquals(132, result.getHorsepower(), "Horsepower should match");
            assertEquals("Benzină", result.getPetrolType(), "Petrol type should match");
            assertEquals("Automat", result.getGearsType(), "Gears type should match");
            assertEquals("Față", result.getTractionType(), "Traction type should match");
            assertEquals("Alb", result.getColor(), "Color should match");
        }
    }

    @Test
    void testExtractDetailedCarInfo_InvalidPrice() throws IOException {
        String carLink = "/car";
        String html = "<div class=\"styles_aside__0m8KW\">" +
                "<span class=\"styles_sidebar__main__DaXQC\">50</span>" +
                "</div>" +
                "<div class=\"styles_features__right__Sn6fV\">" +
                "<div class=\"styles_group__aota8\">" +
                "<ul>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Rulaj</span>" +
                "<span class=\"styles_group__value__XN7OI\">100000 km</span></li>" +
                "</ul></div></div>";
        Document doc = Jsoup.parse(html);

        try (var jsoupMock = mockStatic(Jsoup.class)) {
            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mock(org.jsoup.Connection.class));
            when(Jsoup.connect(anyString()).get()).thenReturn(doc);

            CarDetails result = scraper.extractDetailedCarInfo(carLink);

            assertNull(result, "Should return null for invalid price");
        }
    }

    @Test
    void testExtractDetailedCarInfo_InvalidMileage() throws IOException {
        String carLink = "/car";
        String html = "<div class=\"styles_aside__0m8KW\">" +
                "<span class=\"styles_sidebar__main__DaXQC\">3000</span>" +
                "</div>" +
                "<div class=\"styles_features__right__Sn6fV\">" +
                "<div class=\"styles_group__aota8\">" +
                "<ul>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Rulaj</span>" +
                "<span class=\"styles_group__value__XN7OI\">50 km</span></li>" +
                "</ul></div></div>";
        Document doc = Jsoup.parse(html);

        try (var jsoupMock = mockStatic(Jsoup.class)) {
            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mock(org.jsoup.Connection.class));
            when(Jsoup.connect(anyString()).get()).thenReturn(doc);

            CarDetails result = scraper.extractDetailedCarInfo(carLink);

            assertNull(result, "Should return null for invalid mileage");
        }
    }

    @Test
    void testExtractDetailedCarInfo_InvalidTitle() throws IOException {
        String carLink = "/car";
        String html = "<h1>Dacia Sandero Stepway</h1>" +
                "<div class=\"styles_aside__0m8KW\">" +
                "<span class=\"styles_sidebar__main__DaXQC\">3000</span>" +
                "</div>" +
                "<div class=\"styles_features__right__Sn6fV\">" +
                "<div class=\"styles_group__aota8\">" +
                "<ul>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Rulaj</span>" +
                "<span class=\"styles_group__value__XN7OI\">5000 km</span></li>" +
                "</ul></div></div>";
        Document doc = Jsoup.parse(html);

        try (var jsoupMock = mockStatic(Jsoup.class)) {
            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mock(org.jsoup.Connection.class));
            when(Jsoup.connect(anyString()).get()).thenReturn(doc);

            CarDetails result = scraper.extractDetailedCarInfo(carLink);

            assertNull(result, "Should return null for invalid title");
        }
    }

    @Test
    void testPrintResults_Success() {
        List<CarDetails> finalProducts = new ArrayList<>();
        finalProducts.add(new CarDetails("https://999.md/car1", null, 50, 210000,
                null, "Vând", null, null, null,
                null, null, null, null, null,
                null, null, null, null, null));
        finalProducts.add(new CarDetails("https://999.md/car2", null, 100, 210000,
                null, "Vând", null, null, null,
                null, null, null, null, null,
                null, null, null, null, null));

        scraper.printResults(finalProducts);
        String expectedOutput = String.format("Max price: 100 (Link: https://999.md/car2)%n" +
                "Min price: 50 (Link: https://999.md/car1)%n" +
                "Average price: 75,00%n");
        assertEquals(expectedOutput, outContent.toString(), "Console output should match expected");
    }

    @Test
    void testPrintResults_EmptyList_ThrowsException() {
        List<CarDetails> finalProducts = new ArrayList<>();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> scraper.printResults(finalProducts),
                "Should throw RuntimeException for empty list");
        assertEquals("Product list is empty or null", exception.getMessage());
        assertTrue(outContent.toString().isEmpty(), "No output should be printed");
    }

    @Test
    void testExtractFilterParams_Success() throws IOException{
        String url = "https://999.md/ro/list/transport/cars?appl=1&ef=16,1,6,2200&eo=12885,12900,12912,139,35538&aof=20&o_1_2095_8_98=36188";
        Map<String, String> results = scraper.extractFilterParams(url, "featureId", "optionId");
        assertNotNull(results, "Results should not be null");
        assertEquals("2095", results.get("featureId"), "Feature id should match");
        assertEquals("36188", results.get("optionId"), "Option id should match");
    }

    @Test
    void testExtractFilterParams_QueryWithOnlyQuestionMark_ReturnsEmptyMap() throws IOException {
        String url = "https://999.md/ro/list?";
        Map<String, String> result = scraper.extractFilterParams(url, "featureId", "optionId");
        assertTrue(result.isEmpty(), "Expected empty map for '?' query string");
    }

    @Test
    void testExtractFilterParams_InvalidUrl_ThrowsIOException() {
        String url = "htp://url";
        assertThrows(IOException.class, () -> scraper.extractFilterParams(url, "featureId", "optionId"));
    }

    @Test
    void testGetIntegerFromSection_Success() {
        Map<String, String> map = new HashMap<>();
        map.put("Număr de locuri", "5");
        Integer result = scraper.getIntegerFromSection(map, "Număr de locuri");
        assertEquals(5, result, "The result should be equal");
    }

    @Test
    void testGetIntegerFromSection_InvalidInteger_ThrowsNumberFormatException() {
        Map<String, String> map = new HashMap<>();
        map.put("Număr de locuri", "a");
        assertThrows(NumberFormatException.class, () -> scraper.getIntegerFromSection(map, "Număr de locuri"));
    }

    @Test
    void testGetIntegerFromSection_EmptyMap_ReturnsNull() {
        Map<String, String> map = new HashMap<>();
        Integer result = scraper.getIntegerFromSection(map, "Număr de locuri");
        assertNull(result);
    }

    @Test
    void testGetIntegerFromSection_KeyNotFound_ReturnsNull() {
        Map<String, String>  map = new HashMap<>();
        map.put("Număr uși", "4");
        Integer result = scraper.getIntegerFromSection(map, "Număr de locuri");
        assertNull(result);
    }

    @Test
    void testExtractSection_Success() {
        String html = "<h1>Renault Megane</h1>" +
                "<div class=\"styles_aside__0m8KW\">" +
                "<p class=\"styles_date__voWnk\">Data actualizării: 2023-10-01</p>" +
                "<p class=\"styles_type___J9Dy\">Tip: Vând</p>" +
                "<span class=\"styles_sidebar__main__DaXQC\">15000 €</span>" +
                "<span class=\"styles_address__text__duvKg\">Orhei</span>" +
                "<a class=\"styles_owner__login__VKE71\">John</a>" +
                "</div>" +
                "<div class=\"styles_features__left__ON_QP\">" +
                "<div class=\"styles_group__aota8\">" +
                "<ul>" +
                "<li><span class=\"styles_group__key__uRhnQ\">Generație</span>" +
                "<span class=\"styles_group__value__XN7OI\">III (2008 - 2016)</span></li>" +
                "</ul></div></div>";
        Document doc = Jsoup.parse(html);
        Map<String, String> map = new HashMap<>();
        scraper.extractSection(doc, "div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li", map);
        Map<String, String> expected = Map.of("Generație", "III (2008 - 2016)");
        assertEquals(expected, map);
    }

    @Test
    void testExtractSection_ReturnsEmptyMap() {
        String html = "<h1>Renault Megane</h1>" +
                "<div class=\"styles_aside__0m8KW\">" +
                "<p class=\"styles_date__voWnk\">Data actualizării: 2023-10-01</p>" +
                "<p class=\"styles_type___J9Dy\">Tip: Vând</p>" +
                "<span class=\"styles_sidebar__main__DaXQC\">15000 €</span>" +
                "<span class=\"styles_address__text__duvKg\">Orhei</span>" +
                "<a class=\"styles_owner__login__VKE71\">John</a>" +
                "</div>" +
                "<div class=\"styles_features__left__ON_QP\">" +
                "<div class=\"styles_group__aota8\">" +
                "<ul>" +
                "</ul></div></div>";
        Document doc = Jsoup.parse(html);
        Map<String, String> map = new HashMap<>();
        scraper.extractSection(doc, "div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li", map);
        assertTrue(map.isEmpty());
    }

    @Test
    void testGetEurPrice_Success() {
        String priceText = "2000 €";
        Integer result = scraper.getEurPrice(priceText);
        assertEquals(2000, result);
    }

    @Test
    void testGetEurPrice_PriceNotInEur_ReturnsNull() {
        String priceText = "2000 MDL";
        assertNull(scraper.getEurPrice(priceText));
    }

    @Test
    void testGetEurPrice_ThrowsNumberFormatException() {
        String priceText = "€";
        assertThrows(NumberFormatException.class, () -> scraper.getEurPrice(priceText));
    }

    @Test
    void testGetTitle_Success() {
        String html = "<h1>Title</h1>";
        Document doc = Jsoup.parse(html);
        assertEquals("Title", scraper.getTitle(doc));
    }

    @Test
    void testGetTitle_NoTitle_ReturnsNull() {
        String html = "<div>Title</div>";
        Document doc = Jsoup.parse(html);
        assertNull(scraper.getTitle(doc));
    }

    @Test
    void testGetAdInfo_Success() {
        String html = "<h1>Renault Megane</h1>" +
                "<div class=\"styles_aside__0m8KW\">" +
                "<p class=\"styles_date__voWnk\">Data actualizării: 2023-10-01</p>" +
                "<p class=\"styles_type___J9Dy\">Tip: Vând</p>" +
                "</div>";
        Document doc = Jsoup.parse(html);
        Elements items = doc.select("div.styles_aside__0m8KW");
        assertEquals("Vând", scraper.getAdInfo(items, "p.styles_type___J9Dy"));
    }

    @Test
    void testGetAdInfo_ReturnsNull() {
        String html = "<h1>Renault Megane</h1>" +
                "<div class=\"styles_aside__0m8KW\">" +
                "<p class=\"styles_date__voWnk\">Data actualizării: 2023-10-01</p>" +
                "</div>";
        Document doc = Jsoup.parse(html);
        Elements items = doc.select("div.styles_aside__0m8KW");
        assertNull(scraper.getAdInfo(items, "p.styles_type___J9Dy"));
    }

    @Test
    void testGetString_Success() {
        String html = "<div class=\"styles_aside__0m8KW\">" +
                "<span class=\"styles_sidebar__main__DaXQC\">15000 €</span>" +
                "<span class=\"styles_address__text__duvKg\">Orhei</span>" +
                "<a class=\"styles_owner__login__VKE71\">John</a>" +
                "</div>";
        Document doc = Jsoup.parse(html);
        Elements items = doc.select("div.styles_aside__0m8KW");
        assertEquals("John", scraper.getString(items, "a.styles_owner__login__VKE71"));
    }

    @Test
    void testGetString_ReturnsNull() {
        String html = "<div class=\"styles_aside__0m8KW\">" +
                "<span class=\"styles_sidebar__main__DaXQC\">15000 €</span>" +
                "<span class=\"styles_address__text__duvKg\">Orhei</span>" +
                "</div>";
        Document doc = Jsoup.parse(html);
        Elements items = doc.select("div.styles_aside__0m8KW");
        assertNull(scraper.getString(items, "a.styles_owner__login__VKE71"));
    }

    @Test
    void testCheckFinalProducts_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> scraper.checkFinalProducts(new ArrayList<>()));
    }

    @Test
    void testGetAvgPrice_MinMaxEntry_Success() {
        CarDetails car1 = new CarDetails(null, null, 2000, 100000, null,
                "Vând", null, null, null, null, null, null,
                null, null, null, null, null, null,null);
        CarDetails car2 = new CarDetails(null, null, 8000, 100000, null,
                "Vând", null, null, null, null, null, null,
                null, null, null, null, null, null,null);
        List<CarDetails> list = List.of(car1, car2);
        assertEquals(5000, scraper.getAvgPrice(list, 50000, 200000));
        assertEquals(car1, scraper.getMinEntry(list));
        assertEquals(car2, scraper.getMaxEntry(list));
    }

    @Test
    void testGetAvgPrice_MinMaxEntry_OneBuyAd_Success() {
        CarDetails car1 = new CarDetails(null, null, 2000, 100000, null,
                "Cumpăr", null, null, null, null, null, null,
                null, null, null, null, null, null,null);
        CarDetails car2 = new CarDetails(null, null, 8000, 100000, null,
                "Vând", null, null, null, null, null, null,
                null, null, null, null, null, null,null);
        List<CarDetails> list = List.of(car1, car2);
        assertEquals(8000, scraper.getAvgPrice(list, 50000, 200000));
        assertEquals(car2, scraper.getMinEntry(list));
        assertEquals(car2, scraper.getMaxEntry(list));
    }

    @Test
    void testGetAvgPrice_InvalidMileage() {
        CarDetails car1 = new CarDetails(null, null, 2000, 300000, null,
                "Vând", null, null, null, null, null, null,
                null, null, null, null, null, null,null);
        CarDetails car2 = new CarDetails(null, null, 8000, 100000, null,
                "Vând", null, null, null, null, null, null,
                null, null, null, null, null, null,null);
        List<CarDetails> list = List.of(car1, car2);
        assertEquals(8000, scraper.getAvgPrice(list, 50000, 200000));
    }

    @Test
    void testGetAvgPrice_MinMaxEntry_BuyAds_ThrowsRuntimeException() {
        CarDetails car1 = new CarDetails(null, null, 2000, 100000, null,
                "Cumpăr", null, null, null, null, null, null,
                null, null, null, null, null, null,null);
        CarDetails car2 = new CarDetails(null, null, 8000, 100000, null,
                "Cumpăr", null, null, null, null, null, null,
                null, null, null, null, null, null,null);
        List<CarDetails> list = List.of(car1, car2);
        assertThrows(RuntimeException.class, () -> scraper.getAvgPrice(list, 50000, 200000));
        assertThrows(RuntimeException.class, () -> scraper.getMinEntry(list));
        assertThrows(RuntimeException.class, () -> scraper.getMaxEntry(list));

    }

    @Test
    void testGetAvgPrice_MinMaxEntry_EmptyList_ThrowsRuntimeException() {
        List<CarDetails> list = new ArrayList<>();
        assertThrows(RuntimeException.class, () ->scraper.getAvgPrice(list, 50000, 200000));
        assertThrows(RuntimeException.class, () -> scraper.getMinEntry(list));
        assertThrows(RuntimeException.class, () -> scraper.getMaxEntry(list));
    }
}