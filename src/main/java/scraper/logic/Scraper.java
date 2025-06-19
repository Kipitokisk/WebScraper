package scraper.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import scraper.database.*;
import scraper.database.registry.LookupEntityRegistry;
import scraper.database.registry.ParticularitiesRegistry;
import scraper.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper {
    private final Dotenv dotenv;
    private final Logger logger;
    private final String baseUrl;
    private final String searchUrl;
    private final HttpClient client;
    private final DatabaseManager dbManager;
    private final ExecutorService executor;
    private static final int MIN_MILEAGE = 200000;
    private static final int MAX_MILEAGE = 400000;
    private static final int MAX_EUR_PRICE = 20000;
    private static final long DELAY = 1000L;

    public Scraper(String baseUrl, String searchUrl, DatabaseManager dbManager, Logger logger, HttpClient client, ExecutorService executorService) {
        this.baseUrl = baseUrl;
        this.searchUrl = searchUrl;
        this.logger = logger;
        this.client = client;
        this.dbManager = dbManager;
        this.dotenv = Dotenv.load();
        this.executor = executorService;
    }

    public void scrape() throws IOException, InterruptedException, SQLException {
        List<CarDetails> finalProducts = processCars();
        saveResults(finalProducts);
    }

    List<CarDetails> processCars() throws IOException, InterruptedException {
        String url = dotenv.get("GRAPH_QL_URL");
        String paramFeature = dotenv.get("GRAPH_QL_PARAM_FEATURE");
        String paramOption = dotenv.get("GRAPH_QL_PARAM_OPTION");

        List<String> adIds = fetchAdIds(url, paramFeature, paramOption);

        logger.info("Fetched {} ad IDs", adIds.size());

        List<Future<CarDetails>> futures = extractEachCarDetail(adIds, executor);
        List<CarDetails> carDetails = fetchCarDetails(futures);

        return carDetails;
    }

    List<Future<CarDetails>> extractEachCarDetail(List<String> adIds, ExecutorService executor) {
        List<Future<CarDetails>> futures = new ArrayList<>();
        for (String adId : adIds) {
            String carLink = "/ro/" + adId;
            futures.add(executor.submit(() -> getCarDetails(carLink)));
        }
        return futures;
    }

    CarDetails getCarDetails(String carLink) throws InterruptedException {
        try {
            Thread.sleep(DELAY);
            return getDetails(carLink);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            logger.error("Failed to process car ad {}: {}", carLink, e.getMessage());
            return null;
        }
    }

    CarDetails getDetails(String carLink) throws IOException {
        Document doc = Jsoup.connect(baseUrl + carLink).get();
        String carBrand = dotenv.get("CAR_BRAND");
        String carModel = dotenv.get("CAR_MODEL");
        CarDetails car = new CarDetails(doc, baseUrl, carLink, carBrand, carModel);

        return validateCarDetails(car);
    }

    CarDetails validateCarDetails(CarDetails car) {
        if ((car.getEurPrice() == null) || (car.getEurPrice() > MAX_EUR_PRICE) || (car.getMileage() == null)) {
            return null;
        }

        return car;
    }

    List<CarDetails> fetchCarDetails(List<Future<CarDetails>> futures) throws InterruptedException {
        List<CarDetails> carDetails = new ArrayList<>();
        for (Future<CarDetails> car : futures) {
            getCar(car, carDetails);
        }
        return carDetails;
    }

    void getCar(Future<CarDetails> car, List<CarDetails> carDetails) throws InterruptedException {
        try {
            CarDetails carDetail = car.get();
            addCarDetailToList(carDetails, carDetail);
        } catch (ExecutionException e) {
            logger.error("Error fetching car details: {}", e.getMessage());
        }
    }

    void addCarDetailToList(List<CarDetails> carDetails, CarDetails carDetail) {
        if (carDetail != null) {
            carDetails.add(carDetail);
        }
    }

    List<String> fetchAdIds(String requestUrl, String paramFeature, String paramOption)
            throws IOException, InterruptedException {
        Map<String, String> filterParams = extractFilterParams(searchUrl, paramFeature, paramOption);

        validateFilterParams(paramFeature, paramOption, filterParams);

        String featureId = filterParams.get(paramFeature);
        String optionId = filterParams.get(paramOption);

        String graphQlPayloadTemplate = getGraphQlPayloadTemplate();

        String graphQlPayload = graphQlPayloadTemplate
                .replace("${featureId}", featureId)
                .replace("${optionId}", optionId);

        HttpResponse<String> response = getStringHttpResponse(requestUrl, graphQlPayload);

        JsonNode adsNode = extractAdsNode(response);

        return getAdIds(adsNode);
    }

    List<String> getAdIds(JsonNode adsNode) {
        List<String> adIds = new ArrayList<>();
        Set<String> existingAdIds = getExistingAdIds();
        for (JsonNode ad : adsNode) {
            String adId = ad.path("id").asText();
            if (existingAdIds.contains(baseUrl + adId)) {
                continue;
            }
            adIds.add(adId);
        }
        return adIds;
    }

    Set<String> getExistingAdIds() {
        return CarsMapper.extractLinks(dbManager);
    }

    JsonNode extractAdsNode(HttpResponse<String> response) throws IOException {
        JsonNode rootNode;
        try {
            ObjectMapper mapper = new ObjectMapper();
            rootNode = mapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to parse JSON", e);
        }
        return rootNode.path("data").path("searchAds").path("ads");
    }

    void validateFilterParams(String paramFeature, String paramOption, Map<String, String> filterParams) throws IOException {
        if (!filterParams.containsKey(paramFeature) || !filterParams.containsKey(paramOption)) {
            throw new IOException("Could not extract featureId or optionId from URL");
        }
    }

    String getGraphQlPayloadTemplate() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("graphQlPayload.json")) {
            if (is == null) {
                throw new IOException("Resource graphQlPayload.json not found in classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    HttpResponse<String> getStringHttpResponse(String url, String graphqlPayload)
            throws IOException, InterruptedException {
        HttpRequest request = createRequest(url, graphqlPayload);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP error: " + response.statusCode() + " - " + response.body());
        }

        return response;
    }

    HttpRequest createRequest(String url, String graphqlPayload) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(graphqlPayload))
                .build();
    }

    Map<String, String> extractFilterParams(String searchUrl, String paramFeature, String paramOption)
            throws IOException {
        Map<String, String> params;
        try {
            params = parseUrlElements(searchUrl, paramFeature, paramOption);
        } catch (MalformedURLException e) {
            throw new IOException("Invalid URL format", e);
        }
        return params;
    }

    Map<String, String> parseUrlElements(String searchUrl, String paramFeature, String paramOption)
            throws MalformedURLException {
        URL completeUrl = new URL(searchUrl);
        String query = completeUrl.getQuery();
        return extractParamsFromQuery(paramFeature, paramOption, query);
    }

    Map<String, String> extractParamsFromQuery(String paramFeature, String paramOption, String query) {
        if (query != null) {
            Map<String, String> params = new HashMap<>();
            String[] pairs = query.split("&");
            Pattern pattern = Pattern.compile("o_\\d+_(\\d+)_\\d+_\\d+=(\\d+)");
            for (String pair : pairs) {
                Matcher matcher = pattern.matcher(pair);
                if (matcher.matches()) {
                    params.put(paramFeature, matcher.group(1));
                    params.put(paramOption, matcher.group(2));
                    return params;
                }
            }
        }
        throw new IllegalArgumentException("Query doesn't contain params");
    }

    void printResults(List<CarDetails> finalProducts) {
        checkFinalProducts(finalProducts);

        CarDetails maxEntry = getMaxEntry(finalProducts);
        CarDetails minEntry = getMinEntry(finalProducts);
        double avgPrice = getAvgPrice(finalProducts);

        System.out.println("Max price: " + maxEntry.getEurPrice() + " (Link: " + maxEntry.getLink() + ")");
        System.out.println("Min price: " + minEntry.getEurPrice() + " (Link: " + minEntry.getLink() + ")");
        System.out.printf(Locale.US, "Average price: %.2f%n", avgPrice);
    }

    void checkFinalProducts(List<CarDetails> finalProducts) {
        if (finalProducts == null || finalProducts.isEmpty()) {
            throw new IllegalArgumentException("Product list is empty or null");
        }
    }

    double getAvgPrice(List<CarDetails> finalProducts) {
        return finalProducts.stream()
                .filter(c -> c.getMileage() != null && c.getEurPrice() != null && c.getMileage() > MIN_MILEAGE
                        && c.getMileage() < MAX_MILEAGE && c.getAdType().equals("Vând"))
                .mapToInt(CarDetails::getEurPrice)
                .average()
                .orElseThrow(() -> new RuntimeException("Cannot compute average - list is empty"));
    }

    CarDetails getMinEntry(List<CarDetails> finalProducts) {
        return finalProducts.stream()
                .filter(c -> c.getEurPrice() != null && c.getAdType().equals("Vând"))
                .min(Comparator.comparingInt(CarDetails::getEurPrice))
                .orElseThrow(() -> new RuntimeException("There is no min price"));
    }

    CarDetails getMaxEntry(List<CarDetails> finalProducts) {
        return finalProducts.stream()
                .filter(c -> c.getEurPrice() != null && c.getAdType().equals("Vând"))
                .max(Comparator.comparingInt(CarDetails::getEurPrice))
                .orElseThrow(() -> new RuntimeException("There is no max price"));
    }

    void saveResults(List<CarDetails> finalProducts) throws SQLException {
        if (finalProducts.isEmpty()) {
            logger.info("No products found.");
            return;
        }
        LookupEntityRegistry lookupEntityRegistry = saveLookupEntities(finalProducts);
        ParticularitiesRegistry particularitiesRegistry = saveParticularities(finalProducts, lookupEntityRegistry);
        saveCars(finalProducts, lookupEntityRegistry, particularitiesRegistry);
        printResults(finalProducts);
    }

    void saveCars(List<CarDetails> finalProducts, LookupEntityRegistry lookupEntityRegistry, ParticularitiesRegistry particularitiesRegistry) throws SQLException {
        CarsMapper carsMapper = new CarsMapper(lookupEntityRegistry, particularitiesRegistry, dbManager);
        carsMapper.saveBatch(finalProducts);
    }

    ParticularitiesRegistry saveParticularities(List<CarDetails> finalProducts, LookupEntityRegistry lookupEntityRegistry) throws SQLException {
        ParticularitiesMapper particularitiesMapper = new ParticularitiesMapper(dbManager, lookupEntityRegistry);
        ParticularitiesRegistry particularitiesRegistry =
                new ParticularitiesRegistry(finalProducts, particularitiesMapper);
        particularitiesRegistry.processParticularities();
        return particularitiesRegistry;
    }

    LookupEntityRegistry saveLookupEntities(List<CarDetails> finalProducts) throws SQLException {
        LookupEntityRegistry lookupEntityRegistry = new LookupEntityRegistry(finalProducts, dbManager);
        lookupEntityRegistry.processLookupEntities();
        return lookupEntityRegistry;
    }
}