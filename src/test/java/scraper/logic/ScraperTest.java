package scraper.logic;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.jsoup.nodes.Element;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.Logger;
import scraper.database.DatabaseManager;
import scraper.factory.PlaywrightFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scraper.model.CarDetails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ScraperTest {
    PlaywrightFactory playwrightFactoryMock;
    DatabaseManager databaseManagerMock;
    Logger loggerMock;
    Page pageMock;

    private Scraper scraper;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        playwrightFactoryMock = mock(PlaywrightFactory.class);
        databaseManagerMock = mock(DatabaseManager.class);
        loggerMock = mock(Logger.class);
        pageMock = mock(Page.class);
        scraper = spy(new Scraper(playwrightFactoryMock, "https://999.md", "Renault", "Megane", "III (2008 - 2016)", loggerMock, databaseManagerMock));
        scraper.setPage(pageMock);
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
        String html = "<h1>Renault Megane</h1>" +
                "<div class=\"styles_aside__0m8KW\">" +
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
        String html = "<h1>Renault Megane</h1>" +
                "<div class=\"styles_aside__0m8KW\">" +
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
        assertEquals(car2, scraper.getMinEntry(list));
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
    void testSaveResults_withEmptyFinalProducts() throws SQLException {
        List<CarDetails> emptyList = Collections.emptyList();

        scraper.saveResults(emptyList);

        verify(loggerMock).info("No products found.");
        verify(databaseManagerMock, never()).saveCars(any());
    }

    @Test
    void testExtractCarDetails_noLinkElement() {
        Element carElement = mock(Element.class);
        when(carElement.selectFirst("a.AdPhoto_info__link__OwhY6")).thenReturn(null);

        List<CarDetails> finalProducts = new ArrayList<>();

        scraper.extractCarDetails(carElement, finalProducts);

        assertTrue(finalProducts.isEmpty());
    }

    @Test
    void testExtractCarDetails_withLinkAndNonNullCarDetails() {
        Element carElement = mock(Element.class);
        Element linkElement = mock(Element.class);
        when(carElement.selectFirst("a.AdPhoto_info__link__OwhY6")).thenReturn(linkElement);
        when(linkElement.attr("href")).thenReturn("http://car-link");

        CarDetails mockCarDetails = new CarDetails.Builder().link("https://999.md/car1")
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
                .color(null).build();
        doReturn(mockCarDetails).when(scraper).extractDetailedCarInfo("http://car-link");

        List<CarDetails> finalProducts = new ArrayList<>();

        scraper.extractCarDetails(carElement, finalProducts);

        assertEquals(1, finalProducts.size());
        assertSame(mockCarDetails, finalProducts.get(0));
    }

    @Test
    void testExtractCarDetails_withLinkAndNullCarDetails() {
        Element carElement = mock(Element.class);
        Element linkElement = mock(Element.class);
        when(carElement.selectFirst("a.AdPhoto_info__link__OwhY6")).thenReturn(linkElement);
        when(linkElement.attr("href")).thenReturn("http://car-link");

        doReturn(null).when(scraper).extractDetailedCarInfo("http://car-link");

        List<CarDetails> finalProducts = new ArrayList<>();

        scraper.extractCarDetails(carElement, finalProducts);

        assertTrue(finalProducts.isEmpty());
    }

    @Test
    void testProcessCurrentPage_callsExtractCarDetailsForEachCarElement() {
        String html = "<div class='styles_adlist__3YsgA styles_flex__9wOfD'>" +
                "<div class='AdPhoto_wrapper__gAOIH' id='car1'>" +
                "  <a class='AdPhoto_info__link__OwhY6' href='/car1'></a>" +
                "</div>" +
                "<div class='AdPhoto_wrapper__gAOIH' id='car2'>" +
                "  <a class='AdPhoto_info__link__OwhY6' href='/car2'></a>" +
                "</div>" +
                "</div>";

        when(pageMock.content()).thenReturn(html);
        when(pageMock.waitForSelector(anyString())).thenReturn(null); // Instead of doNothing()
        doNothing().when(scraper).extractCarDetails(any(Element.class), anyList());

        List<CarDetails> finalProducts = new ArrayList<>();

        scraper.processCurrentPage(finalProducts);

        verify(pageMock, times(1)).waitForSelector("div.styles_adlist__3YsgA.styles_flex__9wOfD");

        ArgumentCaptor<Element> elementCaptor = ArgumentCaptor.forClass(Element.class);
        verify(scraper, times(2)).extractCarDetails(elementCaptor.capture(), eq(finalProducts));

        List<Element> capturedElements = elementCaptor.getAllValues();
        assertEquals(2, capturedElements.size());
        assertEquals("car1", capturedElements.get(0).id());
        assertEquals("car2", capturedElements.get(1).id());
    }

    @Test
    void testProcessAllPages_singlePage_buttonNotClicked() {
        doNothing().when(scraper).processCurrentPage(anyList());

        Locator nextButtonMock = mock(Locator.class);
        when(pageMock.getByText("›")).thenReturn(nextButtonMock);
        when(nextButtonMock.isEnabled()).thenReturn(false);

        List<CarDetails> finalProducts = new ArrayList<>();

        scraper.processAllPages(finalProducts);

        verify(nextButtonMock, never()).click();
    }

    @Test
    void testProcessAllPages_twoPages_buttonClickedOnce() {
        doNothing().when(scraper).processCurrentPage(anyList());

        Locator nextButtonMock = mock(Locator.class);
        when(pageMock.getByText("›")).thenReturn(nextButtonMock);
        when(nextButtonMock.isEnabled())
                .thenReturn(true)
                .thenReturn(false);

        List<CarDetails> finalProducts = new ArrayList<>();

        scraper.processAllPages(finalProducts);

        verify(nextButtonMock, times(1)).click();
    }

    @Test
    void testProcessAllPages_threePages_buttonClickedTwice() {
        doNothing().when(scraper).processCurrentPage(anyList());

        Locator nextButtonMock = mock(Locator.class);
        when(pageMock.getByText("›")).thenReturn(nextButtonMock);
        when(nextButtonMock.isEnabled())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        List<CarDetails> finalProducts = new ArrayList<>();

        scraper.processAllPages(finalProducts);

        verify(nextButtonMock, times(2)).click();
    }

    @Test
    void testNavigateToPage_allElementsClicked() {
        Locator transportMock = mock(Locator.class);
        Locator autoturismeMock = mock(Locator.class);
        Locator searchBoxMock = mock(Locator.class);
        Locator modelDivMock = mock(Locator.class);
        Locator generationMock = mock(Locator.class);

        when(pageMock.getByText("Transport")).thenReturn(transportMock);
        when(pageMock.getByText("Autoturisme")).thenReturn(autoturismeMock);
        when(pageMock.getByPlaceholder("Сăutare")).thenReturn(searchBoxMock);
        when(pageMock.locator(contains("Megane"))).thenReturn(modelDivMock);
        when(modelDivMock.locator(contains("III (2008 - 2016)"))).thenReturn(generationMock);

        scraper.navigateToPage();

        verify(pageMock, times(1)).navigate("https://999.md");

        verify(transportMock, times(1)).click();
        verify(autoturismeMock, times(1)).click();
        verify(generationMock, times(1)).click();

        verify(searchBoxMock, times(1)).fill("Renault");
    }

    @Test
    void testScrape_successfulFlow_allMethodsCalled() throws SQLException {
        when(playwrightFactoryMock.createPage()).thenReturn(pageMock);

        doNothing().when(scraper).navigateToPage();
        doNothing().when(scraper).processAllPages(anyList());
        doNothing().when(scraper).saveResults(anyList());

        scraper.scrape();

        InOrder inOrder = inOrder(playwrightFactoryMock, scraper, pageMock, playwrightFactoryMock);
        inOrder.verify(playwrightFactoryMock).createPage();
        inOrder.verify(scraper).navigateToPage();
        inOrder.verify(scraper).processAllPages(any(List.class));
        inOrder.verify(scraper).saveResults(any(List.class));
        inOrder.verify(pageMock).close();
        inOrder.verify(playwrightFactoryMock).close();
    }
}