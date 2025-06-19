package scraper.logic;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import scraper.database.CarsMapper;
import scraper.database.DatabaseManager;
import scraper.model.CarDetails;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScraperTest {

    private Scraper scraper;
    private DatabaseManager mockDbManager;
    private Logger mockLogger;
    private HttpClient mockHttpClient;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        mockDbManager = mock(DatabaseManager.class);
        mockLogger = mock(Logger.class);
        mockHttpClient = mock(HttpClient.class);
        executorService = mock(ExecutorService.class);
        scraper = spy(new Scraper("http://999.md", "https://999.md/search?o_123_456_789_101=654",
                mockDbManager, mockLogger, mockHttpClient, executorService));
    }

    @Test
    void testValidateCarDetails_ValidCar() {
        CarDetails mockCar = mock(CarDetails.class);
        when(mockCar.getEurPrice()).thenReturn(15000);
        when(mockCar.getMileage()).thenReturn(50000);

        CarDetails result = scraper.validateCarDetails(mockCar);

        assertNotNull(result);
        assertEquals(mockCar, result);
    }

    @Test
    void testValidateCarDetails_InvalidPrice() {
        CarDetails mockCar = mock(CarDetails.class);
        when(mockCar.getEurPrice()).thenReturn(25000);
        when(mockCar.getMileage()).thenReturn(50000);

        CarDetails result = scraper.validateCarDetails(mockCar);

        assertNull(result);
    }

    @Test
    void testValidateCarDetails_NullPrice() {
        CarDetails mockCar = mock(CarDetails.class);
        when(mockCar.getEurPrice()).thenReturn(null);
        when(mockCar.getMileage()).thenReturn(50000);

        CarDetails result = scraper.validateCarDetails(mockCar);

        assertNull(result);
    }

    @Test
    void testValidateCarDetails_NullMileage() {
        CarDetails mockCar = mock(CarDetails.class);
        when(mockCar.getEurPrice()).thenReturn(15000);
        when(mockCar.getMileage()).thenReturn(null);

        CarDetails result = scraper.validateCarDetails(mockCar);

        assertNull(result);
    }

    @Test
    void testAddCarDetailToList_ValidCar() {
        List<CarDetails> carDetails = new ArrayList<>();
        CarDetails mockCar = mock(CarDetails.class);

        scraper.addCarDetailToList(carDetails, mockCar);

        assertEquals(1, carDetails.size());
        assertEquals(mockCar, carDetails.get(0));
    }

    @Test
    void testAddCarDetailToList_NullCar() {
        List<CarDetails> carDetails = new ArrayList<>();

        scraper.addCarDetailToList(carDetails, null);

        assertEquals(0, carDetails.size());
    }

    @Test
    void testGetCar_Success() throws InterruptedException, ExecutionException {
        List<CarDetails> carDetails = new ArrayList<>();
        Future<CarDetails> mockFuture = mock(Future.class);
        CarDetails mockCar = mock(CarDetails.class);
        when(mockFuture.get()).thenReturn(mockCar);

        scraper.getCar(mockFuture, carDetails);

        assertEquals(1, carDetails.size());
        assertEquals(mockCar, carDetails.get(0));
    }

    @Test
    void testGetCar_ExecutionException() throws InterruptedException, ExecutionException {
        List<CarDetails> carDetails = new ArrayList<>();
        Future<CarDetails> mockFuture = mock(Future.class);
        when(mockFuture.get()).thenThrow(new ExecutionException("Test error", new RuntimeException()));

        scraper.getCar(mockFuture, carDetails);

        assertEquals(0, carDetails.size());
        verify(mockLogger).error(anyString(), anyString());
    }

    @Test
    void testGetAdIds_ValidJsonNode() {
        JsonNode mockAdsNode = mock(JsonNode.class);
        JsonNode mockAd1 = mock(JsonNode.class);
        JsonNode mockAd2 = mock(JsonNode.class);
        JsonNode mockId1 = mock(JsonNode.class);
        JsonNode mockId2 = mock(JsonNode.class);

        when(mockAdsNode.iterator()).thenReturn(Arrays.asList(mockAd1, mockAd2).iterator());
        when(mockAd1.path("id")).thenReturn(mockId1);
        when(mockAd2.path("id")).thenReturn(mockId2);
        when(mockId1.asText()).thenReturn("12345");
        when(mockId2.asText()).thenReturn("67890");

        doReturn(Collections.emptySet()).when(scraper).getExistingAdIds();
        List<String> result = scraper.getAdIds(mockAdsNode);

        assertEquals(2, result.size());
        assertEquals("12345", result.get(0));
        assertEquals("67890", result.get(1));
    }

    @Test
    void testGetAdIds_EmptyJsonNode() {
        JsonNode mockAdsNode = mock(JsonNode.class);
        when(mockAdsNode.iterator()).thenReturn(Collections.emptyIterator());

        doReturn(Collections.emptySet()).when(scraper).getExistingAdIds();
        List<String> result = scraper.getAdIds(mockAdsNode);

        assertEquals(0, result.size());
    }

    @Test
    void testExtractAdsNode_ValidResponse() throws IOException {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        String jsonResponse = "{\"data\":{\"searchAds\":{\"ads\":[{\"id\":\"123\"}]}}}";
        when(mockResponse.body()).thenReturn(jsonResponse);

        JsonNode result = scraper.extractAdsNode(mockResponse);

        assertNotNull(result);
        assertTrue(result.isArray());
    }

    @Test
    void testExtractAdsNode_InvalidJson() {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn("invalid json");

        assertThrows(IOException.class, () -> scraper.extractAdsNode(mockResponse));
    }

    @Test
    void testValidateFilterParams_ValidParams() {
        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("feature", "123");
        filterParams.put("option", "456");

        assertDoesNotThrow(() -> scraper.validateFilterParams("feature", "option", filterParams));
    }

    @Test
    void testValidateFilterParams_MissingFeature() {
        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("option", "456");

        assertThrows(IOException.class, () ->
                scraper.validateFilterParams("feature", "option", filterParams));
    }

    @Test
    void testExtractParamsFromQuery_ValidQuery() {
        String query = "param1=value1&o_123_456_789_000=999";

        Map<String, String> result = scraper.extractParamsFromQuery("feature", "option", query);

        assertEquals("456", result.get("feature"));
        assertEquals("999", result.get("option"));
    }

    @Test
    void testExtractParamsFromQuery_InvalidQuery() {
        String query = "param1=value1&param2=value2";

        assertThrows(IllegalArgumentException.class, () ->
                scraper.extractParamsFromQuery("feature", "option", query));
    }

    @Test
    void testCheckFinalProducts_ValidList() {
        List<CarDetails> products = Collections.singletonList(mock(CarDetails.class));

        assertDoesNotThrow(() -> scraper.checkFinalProducts(products));
    }

    @Test
    void testCheckFinalProducts_EmptyList() {
        List<CarDetails> products = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> scraper.checkFinalProducts(products));
    }

    @Test
    void testCheckFinalProducts_NullList() {
        assertThrows(IllegalArgumentException.class, () -> scraper.checkFinalProducts(null));
    }

    @Test
    void testGetAvgPrice_ValidCars() {
        CarDetails car1 = mock(CarDetails.class);
        CarDetails car2 = mock(CarDetails.class);

        when(car1.getMileage()).thenReturn(250000);
        when(car1.getEurPrice()).thenReturn(10000);
        when(car1.getAdType()).thenReturn("Vând");

        when(car2.getMileage()).thenReturn(300000);
        when(car2.getEurPrice()).thenReturn(12000);
        when(car2.getAdType()).thenReturn("Vând");

        List<CarDetails> products = Arrays.asList(car1, car2);

        double result = scraper.getAvgPrice(products);

        assertEquals(11000.0, result, 0.01);
    }

    @Test
    void testGetAvgPrice_NoValidCars() {
        CarDetails car1 = mock(CarDetails.class);
        when(car1.getMileage()).thenReturn(100000);
        when(car1.getEurPrice()).thenReturn(10000);
        when(car1.getAdType()).thenReturn("Vând");

        List<CarDetails> products = Arrays.asList(car1);

        assertThrows(RuntimeException.class, () -> scraper.getAvgPrice(products));
    }

    @Test
    void testGetMinEntry_ValidCars() {
        CarDetails car1 = mock(CarDetails.class);
        CarDetails car2 = mock(CarDetails.class);

        when(car1.getEurPrice()).thenReturn(12000);
        when(car1.getAdType()).thenReturn("Vând");

        when(car2.getEurPrice()).thenReturn(10000);
        when(car2.getAdType()).thenReturn("Vând");

        List<CarDetails> products = Arrays.asList(car1, car2);

        CarDetails result = scraper.getMinEntry(products);

        assertEquals(car2, result);
    }

    @Test
    void testGetMinEntry_NoValidCars() {
        CarDetails car1 = mock(CarDetails.class);
        when(car1.getEurPrice()).thenReturn(null);
        when(car1.getAdType()).thenReturn("Vând");

        List<CarDetails> products = Arrays.asList(car1);

        assertThrows(RuntimeException.class, () -> scraper.getMinEntry(products));
    }

    @Test
    void testGetMaxEntry_ValidCars() {
        CarDetails car1 = mock(CarDetails.class);
        CarDetails car2 = mock(CarDetails.class);

        when(car1.getEurPrice()).thenReturn(12000);
        when(car1.getAdType()).thenReturn("Vând");

        when(car2.getEurPrice()).thenReturn(10000);
        when(car2.getAdType()).thenReturn("Vând");

        List<CarDetails> products = Arrays.asList(car1, car2);

        CarDetails result = scraper.getMaxEntry(products);

        assertEquals(car1, result);
    }

    @Test
    void testGetMaxEntry_NoValidCars() {
        CarDetails car1 = mock(CarDetails.class);
        when(car1.getEurPrice()).thenReturn(null);
        when(car1.getAdType()).thenReturn("Vând");

        List<CarDetails> products = Arrays.asList(car1);

        assertThrows(RuntimeException.class, () -> scraper.getMaxEntry(products));
    }

    @Test
    void testSaveResults_EmptyList() throws SQLException {
        scraper.saveResults(new ArrayList<>());

        verify(mockLogger).info("No products found.");
        verifyNoInteractions(mockDbManager);
    }

    @Test
    void testPrintResults() {
        CarDetails car1 = mock(CarDetails.class);
        when(car1.getEurPrice()).thenReturn(1000);
        when(car1.getAdType()).thenReturn("Vând");
        when(car1.getMileage()).thenReturn(150000);

        CarDetails car2 = mock(CarDetails.class);
        when(car2.getEurPrice()).thenReturn(2000);
        when(car2.getAdType()).thenReturn("Vând");
        when(car2.getMileage()).thenReturn(200000);


        CarDetails car3 = mock(CarDetails.class);
        when(car3.getEurPrice()).thenReturn(1500);
        when(car3.getAdType()).thenReturn("Vând");
        when(car3.getMileage()).thenReturn(250000);

        List<CarDetails> cars = Arrays.asList(car1, car2, car3);

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
    void testGetStringHttpResponse() throws IOException, InterruptedException {
        String url = "https://999.md/graphql";
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
        String url = "https://999.md/graphql";
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
    void testScrape() throws IOException, InterruptedException, SQLException {
        CarDetails car1 = mock(CarDetails.class);
        when(car1.getEurPrice()).thenReturn(1000);
        when(car1.getMileage()).thenReturn(150000);

        CarDetails car2 = mock(CarDetails.class);
        when(car2.getEurPrice()).thenReturn(2000);
        when(car2.getMileage()).thenReturn(200000);

        List<CarDetails> cars = Arrays.asList(car1, car2);

        doReturn(cars).when(scraper).processCars();
        doNothing().when(scraper).saveResults(any());

        scraper.scrape();

        verify(scraper).processCars();
        verify(scraper).saveResults(cars);
    }

    @Test
    void testFetchAdIds() throws IOException, InterruptedException {
        String requestUrl = "https://999.md/graphql";
        String paramFeature = "feature";
        String paramOption = "option";

        Map<String, String> mockFilterParams = new HashMap<>();
        mockFilterParams.put("feature", "123");
        mockFilterParams.put("option", "456");

        String mockPayload = "{\"query\": \"test\"}";
        String responseBody = "{\"data\":{\"searchAds\":{\"ads\":[{\"id\":\"123\"},{\"id\":\"456\"}]}}}";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(responseBody);

        doReturn(mockFilterParams).when(scraper)
                .extractFilterParams("https://999.md/search?o_123_456_789_101=654", paramFeature, paramOption);
        doReturn(Collections.emptySet()).when(scraper).getExistingAdIds();
        doReturn(mockPayload).when(scraper).getGraphQlPayloadTemplate();
        doReturn(mockResponse).when(scraper).getStringHttpResponse(eq(requestUrl), anyString());

        List<String> result = scraper.fetchAdIds(requestUrl, paramFeature, paramOption);

        assertEquals(Arrays.asList("123", "456"), result);
    }

    @Test
    void testFetchAdIds_MissingParams() throws IOException {
        String requestUrl = "https://999.md/graphql";
        String paramFeature = "feature";
        String paramOption = "option";

        Map<String, String> mockFilterParams = new HashMap<>();
        mockFilterParams.put("feature", "123");

        doReturn(mockFilterParams).when(scraper)
                .extractFilterParams("https://999.md/search?o_123_456_789_101=654", paramFeature, paramOption);

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

        CarDetails mockCarDetails = mock(CarDetails.class);
        when(mockCarDetails.getEurPrice()).thenReturn(1500);
        when(mockCarDetails.getMileage()).thenReturn(180000);

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

        CarDetails car1 = mock(CarDetails.class);
        when(car1.getEurPrice()).thenReturn(1000);
        when(car1.getMileage()).thenReturn(150000);

        CarDetails car2 = mock(CarDetails.class);
        when(car2.getEurPrice()).thenReturn(2000);
        when(car2.getMileage()).thenReturn(200000);

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

        List<Future<CarDetails>> futures = List.of(mockFuture);

        List<CarDetails> result = scraper.fetchCarDetails(futures);

        assertTrue(result.isEmpty());
        verify(mockLogger).error("Error fetching car details: {}", "Test error");
    }

    @Test
    void testExtractFilterParams() throws IOException {
        String testUrl = "https://999.md/search?o_123_456_789_101=654&other=param";

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
}