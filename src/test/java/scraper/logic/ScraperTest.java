package scraper.logic;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import scraper.database.DatabaseManager;
import scraper.model.CarDetails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ScraperTest {
    private Scraper scraper;
    private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut = System.out;
    private Logger loggerMock;
    private DatabaseManager dbManagerMock;
    private HttpClient clientMock;
    private HttpResponse<String> responseMock;


    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        loggerMock = mock(Logger.class);
        dbManagerMock = mock(DatabaseManager.class);
        clientMock = mock(HttpClient.class);
        responseMock = mock(HttpResponse.class);
        scraper = spy(new Scraper("https://999.md",
                "https://999.md/ro/list/transport/cars?appl=1&ef=16,1,6,2200&eo=12885,12900,12912,139,35538&aof=20&o_1_2095_8_98=36188",
                dbManagerMock, loggerMock, clientMock));
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
            scraper.setCarBrand("Renault");
            scraper.setCarModel("Megane");
            CarDetails result = scraper.extractDetailedCarInfo(carLink);

            assertNotNull(result, "scraper.model.CarDetails should not be null");
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
        finalProducts.add(new CarDetails.Builder().link("https://999.md/car1")
                .name(null)
                .eurPrice(50)
                .mileage(210000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build());
        finalProducts.add(new CarDetails.Builder().link("https://999.md/car2")
                .name(null)
                .eurPrice(100)
                .mileage(210000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build());

        scraper.printResults(finalProducts);
        String expectedOutput = String.format("Max price: 100 (Link: https://999.md/car2)%n" +
                "Min price: 50 (Link: https://999.md/car1)%n" +
                "Average price: 75.00%n");
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
        CarDetails car1 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(2000)
                .mileage(100000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();
        CarDetails car2 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(8000)
                .mileage(100000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();
        List<CarDetails> list = List.of(car1, car2);
        assertEquals(5000, scraper.getAvgPrice(list, 50000, 200000));
        assertEquals(car1, scraper.getMinEntry(list));
        assertEquals(car2, scraper.getMaxEntry(list));
    }

    @Test
    void testGetAvgPrice_MinMaxEntry_OneBuyAd_Success() {
        CarDetails car1 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(2000)
                .mileage(100000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();
        CarDetails car2 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(8000)
                .mileage(100000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();
        List<CarDetails> list = List.of(car1, car2);
        assertEquals(5000, scraper.getAvgPrice(list, 50000, 200000));
        assertEquals(car1, scraper.getMinEntry(list));
        assertEquals(car2, scraper.getMaxEntry(list));
    }

    @Test
    void testGetAvgPrice_InvalidMileage() {
        CarDetails car1 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(2000)
                .mileage(300000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();
        CarDetails car2 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(8000)
                .mileage(100000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();
        List<CarDetails> list = List.of(car1, car2);
        assertEquals(8000, scraper.getAvgPrice(list, 50000, 200000));
    }

    @Test
    void testGetAvgPrice_MinMaxEntry_BuyAds_ThrowsRuntimeException() {
        CarDetails car1 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(2000)
                .mileage(100000)
                .updateDate(null)
                .adType("Cumpăr")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();
        CarDetails car2 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(8000)
                .mileage(100000)
                .updateDate(null)
                .adType("Cumpăr")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();
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

    @Test
    void testExtractEachCarDetail_Success() {
        List<String> adIds = List.of("ad1", "ad2", "ad3");
        List<Future<CarDetails>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        scraper.extractEachCarDetail(adIds, futures, executor);

        assertEquals(3, futures.size(), "Should create 3 futures for 3 ad IDs");

        for (Future<CarDetails> future : futures) {
            assertNotNull(future, "Future should not be null");
        }

        executor.shutdown();
    }

    @Test
    void testExtractEachCarDetail_EmptyAdIds() {
        List<String> emptyAdIds = new ArrayList<>();
        List<Future<CarDetails>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        scraper.extractEachCarDetail(emptyAdIds, futures, executor);

        assertTrue(futures.isEmpty(), "Should create no futures for empty ad IDs");

        executor.shutdown();
    }

    @Test
    void testFetchCarDetails_Success() throws InterruptedException, ExecutionException {
        List<CarDetails> finalProducts = new ArrayList<>();
        List<Future<CarDetails>> futures = new ArrayList<>();

        // Create mock futures
        Future<CarDetails> mockFuture1 = mock(Future.class);
        Future<CarDetails> mockFuture2 = mock(Future.class);

        CarDetails car1 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(2000)
                .mileage(300000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();
        CarDetails car2 = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(8000)
                .mileage(100000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();

        when(mockFuture1.get()).thenReturn(car1);
        when(mockFuture2.get()).thenReturn(car2);

        futures.add(mockFuture1);
        futures.add(mockFuture2);

        scraper.fetchCarDetails(finalProducts, futures);

        assertEquals(2, finalProducts.size(), "Should have 2 cars in final products");
        assertTrue(finalProducts.contains(car1), "Should contain first mock car");
        assertTrue(finalProducts.contains(car2), "Should contain second mock car");
    }

    @Test
    void testFetchCarDetails_WithNullResults() throws InterruptedException, ExecutionException {
        List<CarDetails> finalProducts = new ArrayList<>();
        List<Future<CarDetails>> futures = new ArrayList<>();

        Future<CarDetails> mockFuture1 = mock(Future.class);
        Future<CarDetails> mockFuture2 = mock(Future.class);

        CarDetails car = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(2000)
                .mileage(300000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();

        when(mockFuture1.get()).thenReturn(car);
        when(mockFuture2.get()).thenReturn(null);

        futures.add(mockFuture1);
        futures.add(mockFuture2);

        scraper.fetchCarDetails(finalProducts, futures);

        assertEquals(1, finalProducts.size(), "Should have only 1 car (null results ignored)");
        assertEquals(car, finalProducts.get(0), "Should contain the valid mock car");
    }

    @Test
    void testFetchCarDetails_WithExecutionException() throws InterruptedException, ExecutionException {
        List<CarDetails> finalProducts = new ArrayList<>();
        List<Future<CarDetails>> futures = new ArrayList<>();

        Future<CarDetails> mockFuture1 = mock(Future.class);
        Future<CarDetails> mockFuture2 = mock(Future.class);

        CarDetails car = new CarDetails.Builder().link(null)
                .name(null)
                .eurPrice(2000)
                .mileage(300000)
                .updateDate(null)
                .adType("Vând")
                .region(null)
                .author(null)
                .yearOfFabrication(null)
                .wheelSide(null)
                .nrOfSeats(null)
                .body(null)
                .nrOfDoors(null)
                .engineCapacity(null)
                .horsepower(null)
                .petrolType(null)
                .gearsType(null)
                .tractionType(null)
                .color(null).build();

        when(mockFuture1.get()).thenReturn(car);
        when(mockFuture2.get()).thenThrow(new ExecutionException("Test exception", new RuntimeException()));

        futures.add(mockFuture1);
        futures.add(mockFuture2);

        assertDoesNotThrow(() -> scraper.fetchCarDetails(finalProducts, futures));

        assertEquals(1, finalProducts.size(), "Should have 1 car (exception case ignored)");
        assertEquals(car, finalProducts.get(0), "Should contain the valid mock car");

        assertTrue(outContent.toString().isEmpty() || outContent.toString().contains("Error"),
                "Should log error message or have no console output");
    }

    @Test
    void testFetchCarDetails_EmptyFutures() throws InterruptedException {
        List<CarDetails> finalProducts = new ArrayList<>();
        List<Future<CarDetails>> emptyFutures = new ArrayList<>();

        scraper.fetchCarDetails(finalProducts, emptyFutures);

        assertTrue(finalProducts.isEmpty(), "Should remain empty when no futures provided");
    }

    @Test
    void testParseUrlElements_Success() throws MalformedURLException {
        String url = "https://example.com/search?foo=bar&o_123_456_1_2=789&baz=qux";
        Map<String, String> params = new HashMap<>();

        scraper.parseUrlElements(url, params, "feature", "option");

        assertEquals("456", params.get("feature"));
        assertEquals("789", params.get("option"));
    }

    @Test
    void testParseUrlElements_NoMatchingPattern() throws MalformedURLException {
        String url = "https://example.com/search?foo=bar&baz=qux";
        Map<String, String> params = new HashMap<>();

        scraper.parseUrlElements(url, params, "feature", "option");

        assertTrue(params.isEmpty());
    }

    @Test
    void testParseUrlElements_NoQueryParameters() throws MalformedURLException {
        String url = "https://example.com/search";
        Map<String, String> params = new HashMap<>();

        scraper.parseUrlElements(url, params, "feature", "option");

        assertTrue(params.isEmpty());
    }

    @Test
    void testParseUrlElements_MalformedUrl() {
        String url = "ht!tp://not_a_url";

        assertThrows(MalformedURLException.class, () -> {
            scraper.parseUrlElements(url, new HashMap<>(), "feature", "option");
        });
    }

    @Test
    void testParseUrlElements_NullParamsMap() {
        String url = "https://example.com/search?o_1_100_2_3=200";

        assertThrows(NullPointerException.class, () -> {
            scraper.parseUrlElements(url, null, "feature", "option");
        });
    }

    @Test
    void testSaveResults_withEmptyFinalProducts() throws SQLException {
        List<CarDetails> emptyList = Collections.emptyList();

        scraper.saveResults(emptyList);

        verify(loggerMock).info("No products found.");
        verify(dbManagerMock, never()).saveCars(any());
    }

    @Test
    void testGetStringHttpResponse_success() throws IOException, InterruptedException {
        String url = "https://example.com/graphql";
        String payload = "{\"query\":\"some query\"}";

        HttpResponse<String> responseMock = mock(HttpResponse.class);
        when(responseMock.statusCode()).thenReturn(200);
        when(responseMock.body()).thenReturn("{\"data\":\"ok\"}");

        when(clientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(responseMock);

        HttpResponse<String> response = scraper.getStringHttpResponse(url, payload);

        assertEquals(200, response.statusCode());
        assertEquals("{\"data\":\"ok\"}", response.body());
    }

    @Test
    void testGetStringHttpResponse_httpError() throws IOException, InterruptedException {
        String url = "https://example.com/graphql";
        String payload = "{\"query\":\"some query\"}";

        HttpResponse<String> responseMock = mock(HttpResponse.class);
        when(responseMock.statusCode()).thenReturn(500);
        when(responseMock.body()).thenReturn("Internal Server Error");

        when(clientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(responseMock);

        IOException exception = assertThrows(IOException.class, () -> {
            scraper.getStringHttpResponse(url, payload);
        });

        assertTrue(exception.getMessage().contains("HTTP error: 500"));
        assertTrue(exception.getMessage().contains("Internal Server Error"));
    }

    @Test
    void testGetGraphQlPayloadTemplate_success() throws IOException {
        String payload = scraper.getGraphQlPayloadTemplate();
        assertNotNull(payload);
        assertFalse(payload.isEmpty());
    }

    @Test
    void testFetchAdIds_Success() throws IOException, InterruptedException {
        String mockJsonResponse = "{\"data\":{\"searchAds\":{\"ads\":[{\"id\":\"ad1\"},{\"id\":\"ad2\"}]}}}";
        when(responseMock.body()).thenReturn(mockJsonResponse);
        when(responseMock.statusCode()).thenReturn(200);
        when(clientMock.send(
                ArgumentMatchers.any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())
        ).thenReturn(responseMock);
        when(scraper.getGraphQlPayloadTemplate()).thenReturn("${featureId} ${optionId}");
        doReturn(Map.of("feature", "16", "option", "12885"))
                .when(scraper).extractFilterParams(anyString(), anyString(), anyString());

        List<String> adIds = scraper.fetchAdIds(
                "https://example.com/list?ef=16&eo=12885",
                "feature",
                "option"
        );

        assertNotNull(adIds, "Ad IDs list should not be null");
        assertEquals(2, adIds.size(), "Should return 2 ad IDs");
        assertTrue(adIds.contains("ad1"), "Should contain ad1");
        assertTrue(adIds.contains("ad2"), "Should contain ad2");
    }

    @Test
    void testFetchAdIds_EmptyAdsList() throws IOException, InterruptedException {
        String mockJsonResponse = "{\"data\":{\"searchAds\":{\"ads\":[]}}}";
        when(responseMock.body()).thenReturn(mockJsonResponse);
        when(responseMock.statusCode()).thenReturn(200);
        when(clientMock.send(
                ArgumentMatchers.any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())
        ).thenReturn(responseMock);        when(scraper.getGraphQlPayloadTemplate()).thenReturn("${featureId} ${optionId}");
        doReturn(Map.of("feature", "16", "option", "12885")).when(scraper).extractFilterParams(anyString(), anyString(), anyString());

        List<String> adIds = scraper.fetchAdIds(
                "https://example.com/list?ef=16&eo=12885",
                "feature",
                "option"
        );

        assertNotNull(adIds, "Ad IDs list should not be null");
        assertTrue(adIds.isEmpty(), "Should return an empty list when no ads found");
    }

    @Test
    void testFetchAdIds_HttpError() throws IOException, InterruptedException {
        String mockJsonResponse = "{\"error\":\"Unauthorized\"}";
        when(responseMock.body()).thenReturn(mockJsonResponse);
        when(responseMock.statusCode()).thenReturn(401); // Unauthorized error code
        when(clientMock.send(
                ArgumentMatchers.any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())
        ).thenReturn(responseMock);
        IOException thrown = assertThrows(IOException.class, () -> {
            scraper.fetchAdIds("https://example.com/list?ef=16&eo=12885", "feature", "option");
        });

        assertTrue(thrown.getMessage().contains("HTTP error"));
    }

    @Test
    void testFetchAdIds_MalformedJson() throws IOException, InterruptedException {
        String mockJsonResponse = "This is not JSON";
        when(responseMock.body()).thenReturn(mockJsonResponse);
        when(responseMock.statusCode()).thenReturn(200);
        when(clientMock.send(
                ArgumentMatchers.any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())
        ).thenReturn(responseMock);
        when(scraper.getGraphQlPayloadTemplate()).thenReturn("${featureId} ${optionId}");
        doReturn(Map.of("feature", "16", "option", "12885")).when(scraper).extractFilterParams(anyString(), anyString(), anyString());

        IOException thrown = assertThrows(IOException.class, () -> {
            scraper.fetchAdIds("https://example.com/list?ef=16&eo=12885", "feature", "option");
        });

        assertTrue(thrown.getMessage().contains("Failed to parse JSON"));
    }

    @Test
    void testFetchAdIds_EmptyUrlParams() throws IOException {
        doReturn(Collections.emptyMap()).when(scraper).extractFilterParams(anyString(), anyString(), anyString());

        IOException thrown = assertThrows(IOException.class, () -> {
            scraper.fetchAdIds(
                    "https://example.com/list",
                    "feature",
                    "option"
            );
        });

        assertEquals("Could not extract featureId or optionId from URL", thrown.getMessage());
    }
}