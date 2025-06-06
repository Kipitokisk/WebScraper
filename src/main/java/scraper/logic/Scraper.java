package scraper.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import scraper.model.CarDetails;
import scraper.database.DatabaseManager;

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
    private final DatabaseManager dbManager;
    private final HttpClient client;


    public Scraper(String baseUrl, String searchUrl, DatabaseManager dbManager, Logger logger, HttpClient client) {
        this.baseUrl = baseUrl;
        this.searchUrl = searchUrl;
        this.dbManager = dbManager;
        this.logger = logger;
        this.client = client;
    }

    public void scrape() throws IOException, InterruptedException, SQLException {
        List<CarDetails> finalProducts = new ArrayList<>();
        processCars(finalProducts);
        saveResults(finalProducts);
    }

    void processCars(List<CarDetails> finalProducts) throws IOException, InterruptedException {
        String url = System.getenv("GRAPH_QL_URL");
        String paramFeature = System.getenv("GRAPH_QL_PARAM_FEATURE");
        String paramOption = System.getenv("GRAPH_QL_PARAM_OPTION");
        List<String> adIds = fetchAdIds(url, paramFeature, paramOption);
        List<Future<CarDetails>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        logger.info("Fetched {} ad IDs", adIds.size());

        extractEachCarDetail(adIds, futures, executor);
        fetchCarDetails(finalProducts, futures);
        executor.shutdown();
    }

    void extractEachCarDetail(List<String> adIds, List<Future<CarDetails>> futures, ExecutorService executor) {
        for (String adId : adIds) {
            String carLink = "/ro/" + adId;
            futures.add(executor.submit(() -> extractDetailedCarInfo(carLink)));
        }
    }

    void fetchCarDetails(List<CarDetails> finalProducts, List<Future<CarDetails>> futures) throws InterruptedException {
        for (Future<CarDetails> car : futures) {
            try {
                CarDetails carDetails = car.get();
                if (carDetails != null) finalProducts.add(carDetails);
            } catch (ExecutionException e) {
                logger.error("Error fetching car details: {}", e.getMessage());
            }
        }
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

    CarDetails extractDetailedCarInfo(String carLink) {
        try {
            Document doc = Jsoup.connect(baseUrl + carLink).get();
            
            String title = getTitle(doc);

            Elements items = doc.select("div.styles_aside__0m8KW");
            
            String updateDate = getAdInfo(items, "p.styles_date__voWnk");
            
            String adType = getAdInfo(items, "p.styles_type___J9Dy");
            
            String eurPriceText = getAdInfo(items, "span.styles_sidebar__main__DaXQC");
            Integer eurPrice = getEurPrice(eurPriceText);

            String region = getString(items, "span.styles_address__text__duvKg");

            String author = getString(items, "a.styles_owner__login__VKE71");

            Map<String, String> generalities = new HashMap<>();
            extractSection(doc, "div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li", generalities);

            String generation = generalities.get("Generație");

            Map<String, String> particularities = new HashMap<>();
            extractSection(doc, "div.styles_features__right__Sn6fV > div.styles_group__aota8 > ul > li", particularities);

            Integer yearOfFabrication = getIntegerFromSection(particularities, "An de fabricație");

            String wheelSide = particularities.get("Volan");
            String body = particularities.get("Tip caroserie");
            String color = particularities.get("Culoare");

            Integer nrOfSeats = getIntegerFromSection(particularities, "Număr de locuri");

            Integer nrOfDoors = getIntegerFromSection(particularities, "Număr uși");

            Integer engineCapacity = getIntegerFromSection(particularities, "Capacitate cilindrică");

            Integer horsepower = getIntegerFromSection(particularities, "Putere");

            String petrolType = particularities.get("Tip combustibil");
            String gearsType = particularities.get("Cutie de viteze");
            String tractionType = particularities.get("Tip tracțiune");

            Integer mileage = getIntegerFromSection(particularities, "Rulaj");

            if ((eurPrice == null) || (eurPrice > 20000) || (mileage == null)) {
                return null;
            }

            return new CarDetails.Builder().link(baseUrl + carLink)
                    .name(title + " " + generation)
                    .eurPrice(eurPrice)
                    .mileage(mileage)
                    .updateDate(updateDate)
                    .adType(adType)
                    .region(region)
                    .author(author)
                    .yearOfFabrication(yearOfFabrication)
                    .wheelSide(wheelSide)
                    .nrOfSeats(nrOfSeats)
                    .body(body)
                    .nrOfDoors(nrOfDoors)
                    .engineCapacity(engineCapacity)
                    .horsepower(horsepower)
                    .petrolType(petrolType)
                    .gearsType(gearsType)
                    .tractionType(tractionType)
                    .color(color)
                    .build();
        } catch (IOException e) {
            logger.error("Error fetching car details page, skipping to the next");
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }

    Integer getIntegerFromSection(Map<String, String> map, String element) {
        Integer result = null;
        String text = map.get(element);
        if (text != null) {
            try {
                result = Integer.parseInt(text.replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                throw new NumberFormatException(e.getMessage());
            }
        }
        return result;
    }

    void extractSection(Document doc, String cssQuery, Map<String, String> map) {
        Elements generalitiesItems = doc.select(cssQuery);
        for (Element item : generalitiesItems) {
            Element keyElement = item.selectFirst("span.styles_group__key__uRhnQ");
            Element valueElement = item.selectFirst("span.styles_group__value__XN7OI, a.styles_group__value__XN7OI");
            if (keyElement != null && valueElement != null) {
                map.put(keyElement.text(), valueElement.text());
            }
        }
    }

    Integer getEurPrice(String eurPriceText) {
        Integer result = null;
        if (eurPriceText == null) {
            throw new NullPointerException("Price empty.");
        }
        try {
            if (eurPriceText.contains("€")) {
                result = Integer.parseInt(eurPriceText.replaceAll("\\D", ""));
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException(e.getMessage());
        }
        return result;
    }

    String getTitle(Document doc) {
        String result = null;
        Element titleElement = doc.selectFirst("h1");
        if (titleElement != null) {
            result = titleElement.text();
        }
        return result;
    }

    String getAdInfo(Elements items, String cssQuery) {
        String result = null;
        Element element = items.selectFirst(cssQuery);
        if (element != null) {
            String text = element.text();
            result = text.substring(text.indexOf(":") + 1).trim();
        }
        return result;
    }

    String getString(Elements items, String cssQuery) {
        String result = null;
        Element element = items.selectFirst(cssQuery);
        if (element != null) {
            result = element.text();
        }
        return result;
    }

    void printResults(List<CarDetails> finalProducts) {
        checkFinalProducts(finalProducts);

        CarDetails maxEntry = getMaxEntry(finalProducts);

        CarDetails minEntry = getMinEntry(finalProducts);

        double avgPrice = getAvgPrice(finalProducts, 200000, 400000);

        System.out.println("Max price: " + maxEntry.getEurPrice() + " (Link: " + maxEntry.getLink() + ")");
        System.out.println("Min price: " + minEntry.getEurPrice() + " (Link: " + minEntry.getLink() + ")");
        System.out.printf(Locale.US,"Average price: %.2f%n", avgPrice);
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

    void saveResults(List<CarDetails> finalProducts) throws SQLException{
        if (finalProducts.isEmpty()) {
            logger.info("No products found.");
            return;
        }

        dbManager.saveCars(finalProducts);
        printResults(finalProducts);
    }
}