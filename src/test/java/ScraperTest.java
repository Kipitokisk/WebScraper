import factory.WebDriverFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScraperTest {
    @Mock
    private WebDriverFactory webDriverFactory;
    @Mock
    private DatabaseManager databaseManager;
    @InjectMocks
    private Scraper scraper;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        scraper = new Scraper(webDriverFactory, "https://999.md", "Renault", "Megane", "III (2008 - 2016)");
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }


    @Test
    void testExtractCarDetails_Success() throws IOException {
        List<CarDetails> finalProducts = new ArrayList<>();
        String html = "<div><a class=\"AdPhoto_info__link__OwhY6\" href=\"/car\">Renault Megane 2016</a></div>";
        Element carElement = Jsoup.parse(html).selectFirst("div");

        CarDetails expectedDetails = new CarDetails(
                "https://999.md/ro/car", "Renault Megane 2016", 15000, 100000, "2023-10-01",
                "Vând", "Orhei", "John", 2016, "Stânga", 5, "Sedan", 4, 1800, 132,
                "Benzină", "Automat", "Față", "Alb"
        );

        Scraper spyScraper = spy(scraper);
        doReturn(expectedDetails).when(spyScraper).extractDetailedCarInfo("/car", "Renault Megane 2016");

        spyScraper.extractCarDetails(carElement, finalProducts);

        assertEquals(1, finalProducts.size(), "finalProducts should contain one item");
        assertEquals(expectedDetails, finalProducts.get(0), "CarDetails should match expected");
    }

    @Test
    void testExtractCarDetails_NullLinkElement() throws IOException {
        List<CarDetails> finalProducts = new ArrayList<>();
        String html = "<div></div>";
        Element carElement = Jsoup.parse(html).selectFirst("div");

        scraper.extractCarDetails(carElement, finalProducts);

        assertTrue(finalProducts.isEmpty(), "finalProducts should be empty when no link element is found");
    }

    @Test
    void testExtractDetailedCarInfo_Success() throws IOException {
        String carLink = "/car";
        String carName = "Renault Megane 2016";
        String html = "<div class=\"styles_aside__0m8KW\">" +
                "<p class=\"styles_date__voWnk\">Data actualizării: 2023-10-01</p>" +
                "<p class=\"styles_type___J9Dy\">Tip: Vând</p>" +
                "<span class=\"styles_sidebar__main__DaXQC\">15000</span>" +
                "<span class=\"styles_address__text__duvKg\">Orhei</span>" +
                "<a class=\"styles_owner__login__VKE71\">John</a>" +
                "</div>" +
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

            CarDetails result = scraper.extractDetailedCarInfo(carLink, carName);

            assertNotNull(result, "CarDetails should not be null");
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
        String carName = "Renault Megane 2016";
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

            CarDetails result = scraper.extractDetailedCarInfo(carLink, carName);

            assertNull(result, "Should return null for invalid price");
        }
    }

    @Test
    void testExtractDetailedCarInfo_InvalidMileage() throws IOException {
        // Arrange
        String carLink = "/car";
        String carName = "Renault Megane 2016";
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

            CarDetails result = scraper.extractDetailedCarInfo(carLink, carName);

            assertNull(result, "Should return null for invalid mileage");
        }
    }

    @Test
    void testExtractDetailedCarInfo_InvalidTitle() throws IOException {
        // Arrange
        String carLink = "/car";
        String carName = "Ford Transit 2016";
        String html = "<div class=\"styles_aside__0m8KW\">" +
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

            CarDetails result = scraper.extractDetailedCarInfo(carLink, carName);

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
}
