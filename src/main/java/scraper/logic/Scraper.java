package scraper.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper {
    private final Logger logger;
    private final String baseUrl;
    private final String searchUrl;
    private final HttpClient client;
    private final DatabaseManager dbManager;

    public Scraper(String baseUrl, String searchUrl, DatabaseManager dbManager, Logger logger, HttpClient client) {
        this.baseUrl = baseUrl;
        this.searchUrl = searchUrl;
        this.logger = logger;
        this.client = client;
        this.dbManager = dbManager;
    }

    public void scrape() throws IOException, InterruptedException, SQLException {
        List<CarDetails> finalProducts = processCars();
        saveResults(finalProducts);
    }

    List<CarDetails> processCars() throws IOException, InterruptedException {
        String url = System.getenv("GRAPH_QL_URL");
        String paramFeature = System.getenv("GRAPH_QL_PARAM_FEATURE");
        String paramOption = System.getenv("GRAPH_QL_PARAM_OPTION");
        List<String> adIds = fetchAdIds(url, paramFeature, paramOption);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        logger.info("Fetched {} ad IDs", adIds.size());

        List<Future<CarDetails>> futures = extractEachCarDetail(adIds, executor);
        List<CarDetails> carDetails = fetchCarDetails(futures);
        executor.shutdown();
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
            long delay = 500 + new Random().nextLong(500);
            Thread.sleep(delay);
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
        CarDetails car = new CarDetails(doc, baseUrl, carLink);

        if ((car.getEurPrice() == null) || (car.getEurPrice() > 20000) || (car.getMileage() == null)) {
            return null;
        }

        return car;
    }

    List<CarDetails> fetchCarDetails(List<Future<CarDetails>> futures) throws InterruptedException {
        List<CarDetails> carDetails = new ArrayList<>();
        for (Future<CarDetails> car : futures) {
            try {
                CarDetails carDetail = car.get();
                if (carDetail != null) {
                    carDetails.add(carDetail);
                }
            } catch (ExecutionException e) {
                logger.error("Error fetching car details: {}", e.getMessage());
            }
        }
        return carDetails;
    }

    List<String> fetchAdIds(String requestUrl, String paramFeature, String paramOption) throws IOException, InterruptedException {
        Map<String, String> filterParams = extractFilterParams(searchUrl, paramFeature, paramOption);
        if (!filterParams.containsKey(paramFeature) || !filterParams.containsKey(paramOption)) {
            throw new IOException("Could not extract featureId or optionId from URL");
        }
        String featureId = filterParams.get(paramFeature);
        String optionId = filterParams.get(paramOption);

        String graphQlPayloadTemplate = getGraphQlPayloadTemplate();

        String graphQlPayload = graphQlPayloadTemplate
                .replace("${featureId}", featureId)
                .replace("${optionId}", optionId);

        HttpResponse<String> response = getStringHttpResponse(requestUrl, graphQlPayload);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to parse JSON", e);
        }
        JsonNode adsNode = rootNode.path("data").path("searchAds").path("ads");

        List<String> adIds = new ArrayList<>();
        for (JsonNode ad : adsNode) {
            String adId = ad.path("id").asText();
            adIds.add(adId);
        }
        return adIds;
    }

    String getGraphQlPayloadTemplate() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("graphQlPayload.json")) {
            if (is == null) {
                throw new IOException("Resource graphQlPayload.json not found in classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    HttpResponse<String> getStringHttpResponse(String url, String graphqlPayload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(graphqlPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP error: " + response.statusCode() + " - " + response.body());
        }

        Thread.sleep(500);
        return response;
    }

    Map<String, String> extractFilterParams(String searchUrl, String paramFeature, String paramOption) throws IOException {
        Map<String, String> params = new HashMap<>();
        try {
            parseUrlElements(searchUrl, params, paramFeature, paramOption);
        } catch (MalformedURLException e) {
            throw new IOException("Invalid URL format", e);
        }
        return params;
    }

    void parseUrlElements(String searchUrl, Map<String, String> params, String paramFeature, String paramOption) throws MalformedURLException {
        URL url = new URL(searchUrl);
        String query = url.getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            Pattern pattern = Pattern.compile("o_\\d+_(\\d+)_\\d+_\\d+=(\\d+)");
            for (String pair : pairs) {
                Matcher matcher = pattern.matcher(pair);
                if (matcher.matches()) {
                    params.put(paramFeature, matcher.group(1));
                    params.put(paramOption, matcher.group(2));
                    break;
                }
            }
        }
    }

    void printResults(List<CarDetails> finalProducts) {
        checkFinalProducts(finalProducts);

        CarDetails maxEntry = getMaxEntry(finalProducts);
        CarDetails minEntry = getMinEntry(finalProducts);
        double avgPrice = getAvgPrice(finalProducts, 200000, 400000);

        System.out.println("Max price: " + maxEntry.getEurPrice() + " (Link: " + maxEntry.getLink() + ")");
        System.out.println("Min price: " + minEntry.getEurPrice() + " (Link: " + minEntry.getLink() + ")");
        System.out.printf(Locale.US, "Average price: %.2f%n", avgPrice);
    }

    void checkFinalProducts(List<CarDetails> finalProducts) {
        if (finalProducts == null || finalProducts.isEmpty()) {
            throw new IllegalArgumentException("Product list is empty or null");
        }
    }

    double getAvgPrice(List<CarDetails> finalProducts, int minMileage, int maxMileage) {
        return finalProducts.stream()
                .filter(c -> c.getMileage() != null && c.getEurPrice() != null && c.getMileage() > minMileage &&
                        c.getMileage() < maxMileage && c.getAdType().equals("Vând"))
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
        LookupEntityRegistry lookupEntityRegistry = new LookupEntityRegistry(finalProducts, dbManager);
        lookupEntityRegistry.processLookupEntities();
        ParticularitiesMapper particularitiesMapper = new ParticularitiesMapper(dbManager, lookupEntityRegistry);
        ParticularitiesRegistry particularitiesRegistry = new ParticularitiesRegistry(finalProducts, particularitiesMapper);
        particularitiesRegistry.processParticularities();
        CarsMapper carsMapper = new CarsMapper(lookupEntityRegistry, particularitiesRegistry, dbManager);
        carsMapper.saveBatch(finalProducts);
        printResults(finalProducts);
    }
}