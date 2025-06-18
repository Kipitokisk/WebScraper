package scraper.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import scraper.database.DatabaseManager;
import scraper.model.CarDetails;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScraperTest {

    private Scraper scraper;
    private Logger mockLogger;
    private HttpClient mockHttpClient;
    private DatabaseManager mockDbManager;
    private String searchUrl = "https://999.md/search?o_123_456_789_101=654";

    @BeforeEach
    void setUp() {
        mockLogger = mock(Logger.class);
        mockHttpClient = mock(HttpClient.class);
        mockDbManager = mock(DatabaseManager.class);
        scraper = spy(new Scraper("https://999.md", searchUrl, mockDbManager, mockLogger, mockHttpClient));
    }

    @Test
    void testScrape() throws IOException, InterruptedException, SQLException {
        List<CarDetails> mockCarDetails = Arrays.asList(
                createMockCarDetails(1000, 150000),
                createMockCarDetails(2000, 200000)
        );

        doReturn(mockCarDetails).when(scraper).processCars();
        doNothing().when(scraper).saveResults(any());

        scraper.scrape();

        verify(scraper).processCars();
        verify(scraper).saveResults(mockCarDetails);
    }

    @Test
    void testFetchAdIds() throws IOException, InterruptedException {
        String requestUrl = "https://api.example.com/graphql";
        String paramFeature = "feature";
        String paramOption = "option";

        Map<String, String> mockFilterParams = new HashMap<>();
        mockFilterParams.put("feature", "123");
        mockFilterParams.put("option", "456");

        String mockPayload = "{\"query\": \"test\"}";
        String responseBody = "{\"data\":{\"searchAds\":{\"ads\":[{\"id\":\"123\"},{\"id\":\"456\"}]}}}";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(responseBody);

        doReturn(mockFilterParams).when(scraper).extractFilterParams(searchUrl, paramFeature, paramOption);
        doReturn(mockPayload).when(scraper).getGraphQlPayloadTemplate();
        doReturn(mockResponse).when(scraper).getStringHttpResponse(eq(requestUrl), anyString());

        List<String> result = scraper.fetchAdIds(requestUrl, paramFeature, paramOption);

        assertEquals(Arrays.asList("123", "456"), result);
    }

    @Test
    void testFetchAdIds_MissingParams() throws IOException {
        String requestUrl = "https://api.example.com/graphql";
        String paramFeature = "feature";
        String paramOption = "option";

        Map<String, String> mockFilterParams = new HashMap<>();
        mockFilterParams.put("feature", "123");

        doReturn(mockFilterParams).when(scraper).extractFilterParams(searchUrl, paramFeature, paramOption);

        IOException exception = assertThrows(IOException.class, () ->
                scraper.fetchAdIds(requestUrl, paramFeature, paramOption));

        assertEquals("Could not extract featureId or optionId from URL", exception.getMessage());
    }

    @Test
    void testGetDetails_FilteredOut() {
        CarDetails mockCarDetails = mock(CarDetails.class);
        when(mockCarDetails.getEurPrice()).thenReturn(25000);
        when(mockCarDetails.getMileage()).thenReturn(150000);

        CarDetails result = mockCarDetails;
        if ((result.getEurPrice() == null) || (result.getEurPrice() > 20000) || (result.getMileage() == null)) {
            result = null;
        }

        assertNull(result);
    }

    @Test
    void testGetCarDetails() throws InterruptedException, IOException {
        String carLink = "/ro/123";
        CarDetails mockCarDetails = createMockCarDetails(1500, 180000);

        doReturn(mockCarDetails).when(scraper).getDetails(carLink);

        CarDetails result = scraper.getCarDetails(carLink);

        assertEquals(mockCarDetails, result);
        verify(scraper).getDetails(carLink);
    }

    @Test
    void testGetCarDetails_Exception() throws Exception {
        String carLink = "/ro/123";

        doThrow(new IOException("Network error")).when(scraper).getDetails(carLink);

        CarDetails result = scraper.getCarDetails(carLink);

        assertNull(result);
        verify(mockLogger).error("Failed to process car ad {}: {}", carLink, "Network error");
    }

    @Test
    void testFetchCarDetails() throws InterruptedException, ExecutionException {
        Future<CarDetails> mockFuture1 = mock(Future.class);
        Future<CarDetails> mockFuture2 = mock(Future.class);
        Future<CarDetails> mockFuture3 = mock(Future.class);

        CarDetails car1 = createMockCarDetails(1000, 150000);
        CarDetails car2 = createMockCarDetails(2000, 200000);

        when(mockFuture1.get()).thenReturn(car1);
        when(mockFuture2.get()).thenReturn(car2);
        when(mockFuture3.get()).thenReturn(null);

        List<Future<CarDetails>> futures = Arrays.asList(mockFuture1, mockFuture2, mockFuture3);

        List<CarDetails> result = scraper.fetchCarDetails(futures);

        assertEquals(2, result.size());
        assertTrue(result.contains(car1));
        assertTrue(result.contains(car2));
    }

    @Test
    void testFetchCarDetails_ExecutionException() throws InterruptedException, ExecutionException {
        Future<CarDetails> mockFuture = mock(Future.class);
        when(mockFuture.get()).thenThrow(new ExecutionException("Test error", new RuntimeException()));

        List<Future<CarDetails>> futures = Arrays.asList(mockFuture);

        List<CarDetails> result = scraper.fetchCarDetails(futures);

        assertTrue(result.isEmpty());
        verify(mockLogger).error("Error fetching car details: {}", "Test error");
    }

    @Test
    void testExtractFilterParams() throws IOException {
        String testUrl = "https://example.com/search?o_123_456_789_101=654&other=param";

        Map<String, String> result = scraper.extractFilterParams(testUrl, "feature", "option");

        assertEquals("456", result.get("feature"));
        assertEquals("654", result.get("option"));
    }

    @Test
    void testParseUrlElements() throws Exception {
        String testUrl = "https://999.md/search?o_123_456_789_101=654";
        Map<String, String> params = scraper.parseUrlElements(testUrl, "feature", "option");

        assertEquals("456", params.get("feature"));
        assertEquals("654", params.get("option"));
    }

    @Test
    void testGetStringHttpResponse() throws IOException, InterruptedException {
        String url = "https://api.example.com/graphql";
        String payload = "{\"query\": \"test\"}";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"data\": {}}");

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        HttpResponse<String> result = scraper.getStringHttpResponse(url, payload);

        assertEquals(mockResponse, result);
        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testGetStringHttpResponse_HttpError() throws IOException, InterruptedException {
        String url = "https://api.example.com/graphql";
        String payload = "{\"query\": \"test\"}";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("Internal Server Error");

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        IOException exception = assertThrows(IOException.class, () ->
                scraper.getStringHttpResponse(url, payload));

        assertEquals("HTTP error: 500 - Internal Server Error", exception.getMessage());
    }

    @Test
    void testPrintResults() {
        List<CarDetails> cars = Arrays.asList(
                createMockCarDetails(1000, 150000),
                createMockCarDetails(2000, 200000),
                createMockCarDetails(1500, 250000)
        );

        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        scraper.printResults(cars);

        String output = outContent.toString();
        assertTrue(output.contains("Max price: 2000"));
        assertTrue(output.contains("Min price: 1000"));
        assertTrue(output.contains("Average price: 1500.00"));

        System.setOut(System.out);
    }

    @Test
    void testCheckFinalProducts_EmptyList() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                scraper.checkFinalProducts(new ArrayList<>()));

        assertEquals("Product list is empty or null", exception.getMessage());
    }

    @Test
    void testCheckFinalProducts_NullList() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                scraper.checkFinalProducts(null));

        assertEquals("Product list is empty or null", exception.getMessage());
    }

    @Test
    void testGetAvgPrice() {
        List<CarDetails> cars = Arrays.asList(
                createMockCarDetails(1000, 250000),
                createMockCarDetails(2000, 300000),
                createMockCarDetails(1500, 350000),
                createMockCarDetails(3000, 150000)
        );

        double avgPrice = scraper.getAvgPrice(cars);

        assertEquals(1500.0, avgPrice, 0.01);
    }

    @Test
    void testGetAvgPrice_NoValidCars() {
        List<CarDetails> cars = Arrays.asList(
                createMockCarDetails(1000, 150000),
                createMockCarDetails(2000, 450000)
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                scraper.getAvgPrice(cars));

        assertEquals("Cannot compute average - list is empty", exception.getMessage());
    }

    @Test
    void testGetMinEntry() {
        List<CarDetails> cars = Arrays.asList(
                createMockCarDetails(2000, 200000),
                createMockCarDetails(1000, 150000),
                createMockCarDetails(1500, 300000)
        );

        CarDetails minEntry = scraper.getMinEntry(cars);

        assertEquals(1000, minEntry.getEurPrice().intValue());
    }

    @Test
    void testGetMaxEntry() {
        List<CarDetails> cars = Arrays.asList(
                createMockCarDetails(2000, 200000),
                createMockCarDetails(1000, 150000),
                createMockCarDetails(1500, 300000)
        );

        CarDetails maxEntry = scraper.getMaxEntry(cars);

        assertEquals(2000, maxEntry.getEurPrice().intValue());
    }

    @Test
    void testSaveResults_EmptyList() throws SQLException {
        scraper.saveResults(new ArrayList<>());

        verify(mockLogger).info("No products found.");
        verifyNoInteractions(mockDbManager);
    }

    private CarDetails createMockCarDetails(Integer price, Integer mileage) {
        CarDetails carDetails = mock(CarDetails.class);
        when(carDetails.getEurPrice()).thenReturn(price);
        when(carDetails.getMileage()).thenReturn(mileage);
        when(carDetails.getAdType()).thenReturn("Vând");
        when(carDetails.getLink()).thenReturn("https://999.md/car/" + price);
        return carDetails;
    }
}