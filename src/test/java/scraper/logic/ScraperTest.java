//package scraper.logic;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import scraper.database.DatabaseManager;
//import scraper.model.CarDetails;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.charset.StandardCharsets;
//import java.sql.SQLException;
//import java.util.*;
//import java.util.concurrent.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class ScraperTest {
//
//    private Logger logger;
//    private HttpClient client;
//    private DatabaseManager dbManager;
//    private Document document;
//    private HttpResponse<String> httpResponse;
//    private JsonNode jsonNode;
//    private ObjectMapper objectMapper;
//
//    private Scraper scraper;
//
//    private final String baseUrl = "http://example.com";
//    private final String searchUrl = "http://example.com/search?o_123_456_789=101";
//    private final String graphQlUrl = "http://graphql.example.com";
//
//    @BeforeEach
//    void setUp() {
//        logger = mock(Logger.class);
//        client = mock(HttpClient.class);
//        dbManager = mock(DatabaseManager.class);
//        document = mock(Document.class);
//        httpResponse = mock(HttpResponse.class);
//        jsonNode = mock(JsonNode.class);
//        objectMapper = mock(ObjectMapper.class);
//
//        scraper = new Scraper(baseUrl, searchUrl, dbManager, logger, client);
//
//        // Mock system environment variables
//        System.setProperty("GRAPH_QL_URL", graphQlUrl);
//        System.setProperty("GRAPH_QL_PARAM_FEATURE", "featureId");
//        System.setProperty("GRAPH_QL_PARAM_OPTION", "optionId");
//    }
//
//    @Test
//    void testScrape_SuccessfulExecution() throws IOException, InterruptedException, SQLException {
//        // Arrange
//        List<String> adIds = List.of("1", "2");
//        CarDetails car1 = mock(CarDetails.class);
//        CarDetails car2 = mock(CarDetails.class);
//        when(car1.getEurPrice()).thenReturn(15000);
//        when(car1.getMileage()).thenReturn(100000);
//        when(car1.getAdType()).thenReturn("Vând");
//        when(car1.getLink()).thenReturn(baseUrl + "/ro/1");
//        when(car2.getEurPrice()).thenReturn(18000);
//        when(car2.getMileage()).thenReturn(120000);
//        when(car2.getAdType()).thenReturn("Vând");
//        when(car2.getLink()).thenReturn(baseUrl + "/ro/2");
//
//        // Mock fetchAdIds
//        when(httpResponse.statusCode()).thenReturn(200);
//        when(httpResponse.body()).thenReturn("{\"data\":{\"searchAds\":{\"ads\":[{\"id\":\"1\"},{\"id\":\"2\"}]}}");
//        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
//        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
//        JsonNode dataNode = mock(JsonNode.class);
//        JsonNode searchAdsNode = mock(JsonNode.class);
//        JsonNode adsNode = mock(JsonNode.class);
//        when(jsonNode.path("data")).thenReturn(dataNode);
//        when(dataNode.path("searchAds")).thenReturn(searchAdsNode);
//        when(searchAdsNode.path("ads")).thenReturn(adsNode);
//        when(adsNode.iterator()).thenReturn(List.of(mock(JsonNode.class), mock(JsonNode.class)).iterator());
//        when(adsNode.iterator().next().path("id").asText()).thenReturn("1").thenReturn("2");
//
//        // Mock Jsoup connection
//        when(Jsoup.connect(anyString())).thenReturn(mock(org.jsoup.Connection.class));
//        when(Jsoup.connect(anyString()).get()).thenReturn(document);
//
//        // Act
//        scraper.scrape();
//
//        // Assert
//        verify(client, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
//        verify(logger, atLeastOnce()).info(contains("Fetched 2 ad IDs"));
//        verify(dbManager, atLeastOnce()).getConnection(); // Assuming used in saveResults
//    }
//
//    @Test
//    void testFetchAdIds_Success() throws IOException, InterruptedException {
//        // Arrange
//        String payloadTemplate = "{\"query\":\"query($featureId: String, $optionId: String) { searchAds(featureId: $featureId, optionId: $optionId) { ads { id } }}\"}";
//        try (var is = new ByteArrayInputStream(payloadTemplate.getBytes(StandardCharsets.UTF_8))) {
//            when(Scraper.class.getClassLoader().getResourceAsStream("graphQlPayload.json")).thenReturn(is);
//        }
//        when(httpResponse.statusCode()).thenReturn(200);
//        when(httpResponse.body()).thenReturn("{\"data\":{\"searchAds\":{\"ads\":[{\"id\":\"1\"},{\"id\":\"2\"}]}}");
//        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
//        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
//        JsonNode dataNode = mock(JsonNode.class);
//        JsonNode searchAdsNode = mock(JsonNode.class);
//        JsonNode adsNode = mock(JsonNode.class);
//        when(jsonNode.path("data")).thenReturn(dataNode);
//        when(dataNode.path("searchAds")).thenReturn(searchAdsNode);
//        when(searchAdsNode.path("ads")).thenReturn(adsNode);
//        when(adsNode.iterator()).thenReturn(List.of(mock(JsonNode.class), mock(JsonNode.class)).iterator());
//        when(adsNode.iterator().next().path("id").asText()).thenReturn("1").thenReturn("2");
//
//        // Act
//        List<String> adIds = scraper.fetchAdIds(graphQlUrl, "featureId", "optionId");
//
//        // Assert
//        assertEquals(List.of("1", "2"), adIds);
//        verify(client, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
//        verify(logger, never()).error(anyString(), any());
//    }
//
//    @Test
//    void testFetchAdIds_HttpError_ThrowsIOException() throws IOException, InterruptedException {
//        // Arrange
//        String payloadTemplate = "{\"query\":\"query($featureId: String, $optionId: String) { searchAds(featureId: $featureId, optionId: $optionId) { ads { id } }}\"}";
//        try (var is = new ByteArrayInputStream(payloadTemplate.getBytes(StandardCharsets.UTF_8))) {
//            when(Scraper.class.getClassLoader().getResourceAsStream("graphQlPayload.json")).thenReturn(is);
//        }
//        when(httpResponse.statusCode()).thenReturn(500);
//        when(httpResponse.body()).thenReturn("Server error");
//        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
//
//        // Act & Assert
//        IOException exception = assertThrows(IOException.class, () -> scraper.fetchAdIds(graphQlUrl, "featureId", "optionId"));
//        assertTrue(exception.getMessage().contains("HTTP error: 500"));
//        verify(logger, atLeastOnce()).error(anyString(), any());
//    }
//
//    @Test
//    void testFetchAdIds_InvalidJson_ThrowsIOException() throws IOException, InterruptedException {
//        // Arrange
//        String payloadTemplate = "{\"query\":\"query($featureId: String, $optionId: String) { searchAds(featureId: $featureId, optionId: $optionId) { ads { id } }}\"}";
//        try (var is = new ByteArrayInputStream(payloadTemplate.getBytes(StandardCharsets.UTF_8))) {
//            when(Scraper.class.getClassLoader().getResourceAsStream("graphQlPayload.json")).thenReturn(is);
//        }
//        when(httpResponse.statusCode()).thenReturn(200);
//        when(httpResponse.body()).thenReturn("invalid json");
//        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
//        when(objectMapper.readTree("invalid json")).thenThrow(new IOException("Invalid JSON"));
//
//        // Act & Assert
//        assertThrows(IOException.class, () -> scraper.fetchAdIds(graphQlUrl, "featureId", "optionId"));
//        verify(logger, atLeastOnce()).error(anyString(), any());
//    }
//
//    @Test
//    void testGetCarDetails_Success() throws InterruptedException, IOException {
//        // Arrange
//        String carLink = "/ro/1";
//        when(Jsoup.connect(baseUrl + carLink)).thenReturn(mock(org.jsoup.Connection.class));
//        when(Jsoup.connect(baseUrl + carLink).get()).thenReturn(document);
//        CarDetails carDetails = mock(CarDetails.class);
//        when(carDetails.getEurPrice()).thenReturn(15000);
//        when(carDetails.getMileage()).thenReturn(100000);
//
//        // Act
//        CarDetails result = scraper.getCarDetails(carLink);
//
//        // Assert
//        assertNotNull(result);
//        verify(logger, never()).error(anyString(), any());
//    }
//
//    @Test
//    void testGetCarDetails_HighPrice_ReturnsNull() throws InterruptedException, IOException {
//        // Arrange
//        String carLink = "/ro/1";
//        when(Jsoup.connect(baseUrl + carLink)).thenReturn(mock(org.jsoup.Connection.class));
//        when(Jsoup.connect(baseUrl + carLink).get()).thenReturn(document);
//        CarDetails carDetails = mock(CarDetails.class);
//        when(carDetails.getEurPrice()).thenReturn(25000); // Above threshold
//        when(carDetails.getMileage()).thenReturn(100000);
//
//        // Act
//        CarDetails result = scraper.getCarDetails(carLink);
//
//        // Assert
//        assertNull(result);
//        verify(logger, atLeastOnce()).error(anyString(), any());
//    }
//
//    @Test
//    void testExtractEachCarDetail_SubmitsTasks() {
//        // Arrange
//        List<String> adIds = List.of("1", "2");
//        ExecutorService executor = mock(ExecutorService.class);
//        when(executor.submit(any(Callable.class))).thenReturn(mock(Future.class));
//
//        // Act
//        List<Future<CarDetails>> futures = scraper.extractEachCarDetail(adIds, executor);
//
//        // Assert
//        assertEquals(2, futures.size());
//        verify(executor, times(2)).submit(any(Callable.class));
//    }
//
//    @Test
//    void testFetchCarDetails_ProcessesFutures() throws InterruptedException, ExecutionException {
//        // Arrange
//        Future<CarDetails> future1 = mock(Future.class);
//        Future<CarDetails> future2 = mock(Future.class);
//        CarDetails car1 = mock(CarDetails.class);
//        when(future1.get()).thenReturn(car1);
//        when(future2.get()).thenReturn(null); // Simulating a failed fetch
//        List<Future<CarDetails>> futures = List.of(future1, future2);
//
//        // Act
//        List<CarDetails> result = scraper.fetchCarDetails(futures);
//
//        // Assert
//        assertEquals(1, result.size());
//        assertEquals(car1, result.get(0));
//        verify(logger, never()).error(anyString(), any());
//    }
//
//    @Test
//    void testExtractFilterParams_Success() throws IOException {
//        // Act
//        Map<String, String> params = scraper.extractFilterParams(searchUrl, "featureId", "optionId");
//
//        // Assert
//        assertEquals("456", params.get("featureId"));
//        assertEquals("101", params.get("optionId"));
//    }
//
//    @Test
//    void testExtractFilterParams_InvalidUrl_ThrowsIOException() {
//        // Arrange
//        String invalidUrl = "http://example.com/invalid";
//
//        // Act & Assert
//        assertThrows(IOException.class, () -> scraper.extractFilterParams(invalidUrl, "featureId", "optionId"));
//    }
//
//    @Test
//    void testGetAvgPrice_ValidData() {
//        // Arrange
//        CarDetails car1 = mock(CarDetails.class);
//        CarDetails car2 = mock(CarDetails.class);
//        when(car1.getEurPrice()).thenReturn(15000);
//        when(car1.getMileage()).thenReturn(250000);
//        when(car1.getAdType()).thenReturn("Vând");
//        when(car2.getEurPrice()).thenReturn(18000);
//        when(car2.getMileage()).thenReturn(300000);
//        when(car2.getAdType()).thenReturn("Vând");
//        List<CarDetails> cars = List.of(car1, car2);
//
//        // Act
//        double avgPrice = scraper.getAvgPrice(cars, 200000, 400000);
//
//        // Assert
//        assertEquals(16500.0, avgPrice, 0.01);
//    }
//
//    @Test
//    void testGetAvgPrice_EmptyFilteredList_ThrowsRuntimeException() {
//        // Arrange
//        CarDetails car = mock(CarDetails.class);
//        when(car.getEurPrice()).thenReturn(15000);
//        when(car.getMileage()).thenReturn(100000); // Outside range
//        when(car.getAdType()).thenReturn("Vând");
//        List<CarDetails> cars = List.of(car);
//
//        // Act & Assert
//        assertThrows(RuntimeException.class, () -> scraper.getAvgPrice(cars, 200000, 400000));
//    }
//
//    @Test
//    void testGetMinEntry_Success() {
//        // Arrange
//        CarDetails car1 = mock(CarDetails.class);
//        CarDetails car2 = mock(CarDetails.class);
//        when(car1.getEurPrice()).thenReturn(15000);
//        when(car1.getAdType()).thenReturn("Vând");
//        when(car2.getEurPrice()).thenReturn(18000);
//        when(car2.getAdType()).thenReturn("Vând");
//        List<CarDetails> cars = List.of(car1, car2);
//
//        // Act
//        CarDetails minEntry = scraper.getMinEntry(cars);
//
//        // Assert
//        assertEquals(car1, minEntry);
//    }
//
//    @Test
//    void testGetMinEntry_NoValidCars_ThrowsRuntimeException() {
//        // Arrange
//        CarDetails car = mock(CarDetails.class);
//        when(car.getEurPrice()).thenReturn(null);
//        when(car.getAdType()).thenReturn("Vând");
//        List<CarDetails> cars = List.of(car);
//
//        // Act & Assert
//        assertThrows(RuntimeException.class, () -> scraper.getMinEntry(cars));
//    }
//
//    @Test
//    void testCheckFinalProducts_EmptyList_ThrowsIllegalArgumentException() {
//        // Act & Assert
//        assertThrows(IllegalArgumentException.class, () -> scraper.checkFinalProducts(Collections.emptyList()));
//    }
//}