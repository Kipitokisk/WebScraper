import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper {
    private static final Logger logger = LoggerFactory.getLogger(Scraper.class);
    private final String base_url;
    private final String search_url;

    private final DatabaseManager dbManager;

    public Scraper(String base_url, String search_url) {
        this.base_url = base_url;
        this.search_url = search_url;
        this.dbManager = new DatabaseManager("jdbc:postgresql://http-postgres-db:5432/scraper_db", "postgres", "pass");
    }

    public void scrape() throws IOException, InterruptedException {
        List<CarDetails> finalProducts = new ArrayList<>();
        List<Future<CarDetails>> futures = new ArrayList<>();
        try {
            processCars(futures, finalProducts);
            saveResults(finalProducts);
        } catch (IOException | InterruptedException e) {
            logger.error("Error during scraping: {}", e.getMessage());
            throw e;
        }
    }

    private void processCars(List<Future<CarDetails>> futures, List<CarDetails> finalProducts) throws IOException, InterruptedException {
        List<String> adIds = fetchAdIds();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        logger.info("Fetched {} ad IDs", adIds.size());

        for (String adId : adIds) {
            String carLink = "/ro/" + adId;
            futures.add(executor.submit(() -> extractDetailedCarInfo(carLink)));
        }
        for (Future<CarDetails> car : futures) {
            try {
                CarDetails carDetails = car.get();
                if (carDetails != null) finalProducts.add(carDetails);
            } catch (ExecutionException e) {
                logger.error("Error fetching car details: {}", e.getMessage());
            }
        }
        executor.shutdown();
    }

    private List<String> fetchAdIds() throws IOException, InterruptedException {
        String url = "https://999.md/graphql";

        Map<String, String> filterParams = extractFilterParams(search_url);
        if (!filterParams.containsKey("featureId") || !filterParams.containsKey("optionId")) {
            throw new IOException("Could not extract featureId or optionId from URL");
        }
        String featureId = filterParams.get("featureId");
        String optionId = filterParams.get("optionId");

        String graphqlPayload = String.format("""
            {
                "operationName": "SearchAds",
                "variables": {
                    "isWorkCategory": false,
                    "includeCarsFeatures": true,
                    "includeBody": false,
                    "includeOwner": true,
                    "includeBoost": false,
                    "input": {
                        "subCategoryId": 659,
                        "source": "AD_SOURCE_MOBILE",
                        "pagination": {
                            "limit": 2000,
                            "skip": 0
                        },
                        "filters": [
                            {
                                "filterId": 1,
                                "features": [
                                    {
                                        "featureId": %s,
                                        "optionIds": [%s]
                                    }
                                ]
                            }
                        ]
                    },
                    "locale": "ro_RO"
                },
                "query": "query SearchAds($input: Ads_SearchInput!, $isWorkCategory: Boolean = false, $includeCarsFeatures: Boolean = false, $includeBody: Boolean = false, $includeOwner: Boolean = false, $includeBoost: Boolean = false, $locale: Common_Locale) {\\n  searchAds(input: $input) {\\n    ads {\\n      ...AdsSearchFragment\\n      __typename\\n    }\\n    count\\n    __typename\\n  }\\n}\\n\\nfragment AdsSearchFragment on Advert {\\n  ...AdListFragment\\n  ...WorkCategoryFeatures @include(if: $isWorkCategory)\\n  reseted(\\n    input: {format: \\"2 Jan. 2006, 15:04\\", locale: $locale, timezone: \\"Europe/Chisinau\\", getDiff: false}\\n  )\\n  __typename\\n}\\n\\nfragment AdListFragment on Advert {\\n  id\\n  title\\n  subCategory {\\n    ...CategoryAdFragment\\n    __typename\\n  }\\n  ...PriceAndImages\\n  ...CarsFeatures @include(if: $includeCarsFeatures)\\n  ...AdvertOwner @include(if: $includeOwner)\\n  transportYear: feature(id: 19) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  realEstate: feature(id: 795) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  body: feature(id: 13) @include(if: $includeBody) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  ...AdvertBooster @include(if: $includeBoost)\\n  label: displayProduct(alias: LABEL) {\\n    ... on DisplayLabel {\\n      enable\\n      ...DisplayLabelFragment\\n      __typename\\n    }\\n    __typename\\n  }\\n  folded: displayProduct(alias: FRAME) {\\n    ... on DisplayFrame {\\n      enable\\n      __typename\\n    }\\n    __typename\\n  }\\n  animation: displayProduct(alias: ANIMATION) {\\n    ... on DisplayAnimation {\\n      enable\\n      __typename\\n    }\\n    __typename\\n  }\\n  animationAndFrame: displayProduct(alias: ANIMATION_AND_FRAME) {\\n    ... on DisplayAnimationAndFrame {\\n      enable\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment CategoryAdFragment on Category {\\n  id\\n  title {\\n    ...TranslationFragment\\n    __typename\\n  }\\n  parent {\\n    id\\n    title {\\n      ...TranslationFragment\\n    __typename\\n    }\\n    parent {\\n      id\\n      title {\\n        ...TranslationFragment\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment TranslationFragment on I18NTr {\\n  translated\\n  __typename\\n}\\n\\nfragment PriceAndImages on Advert {\\n  price: feature(id: 2) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  pricePerMeter: feature(id: 1385) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  images: feature(id: 14) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment FeatureValueFragment on FeatureValue {\\n  id\\n  type\\n  value\\n  __typename\\n}\\n\\nfragment CarsFeatures on Advert {\\n  carFuel: feature(id: 151) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  carDrive: feature(id: 108) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  carTransmission: feature(id: 101) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  mileage: feature(id: 104) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  engineVolume: feature(id: 103) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment AdvertOwner on Advert {\\n  owner {\\n    business {\\n      plan\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment AdvertBooster on Advert {\\n  booster: product(alias: BOOSTER) {\\n    enable\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment DisplayLabelFragment on DisplayLabel {\\n  title\\n  color {\\n    ...ColorFragment\\n    __typename\\n  }\\n  gradient {\\n    ...GradientFragment\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment ColorFragment on Common_Color {\\n  r\\n  g\\n  b\\n  a\\n  __typename\\n}\\n\\nfragment GradientFragment on Gradient {\\n  from {\\n    ...ColorFragment\\n    __typename\\n  }\\n  to {\\n    ...ColorFragment\\n    __typename\\n  }\\n  position\\n  rotation\\n  __typename\\n}\\n\\nfragment WorkCategoryFeatures on Advert {\\n  salary: feature(id: 266) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  workSchedule: feature(id: 260) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  workExperience: feature(id: 263) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  education: feature(id: 261) {\\n    ...FeatureValueFragment\\n    __typename\\n  }\\n  __typename\\n}"
            }""", featureId, optionId);

        HttpResponse<String> response = getStringHttpResponse(url, graphqlPayload);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.body());
        JsonNode adsNode = rootNode.path("data").path("searchAds").path("ads");

        List<String> adIds = new ArrayList<>();
        for (JsonNode ad : adsNode) {
            String adId = ad.path("id").asText();
            adIds.add(adId);
        }
        return adIds;
    }

    private static HttpResponse<String> getStringHttpResponse(String url, String graphqlPayload) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(graphqlPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.error("GraphQL request failed with status {}: {}", response.statusCode(), response.body());
            throw new IOException("HTTP error: " + response.statusCode() + " - " + response.body());
        }
        return response;
    }

    Map<String, String> extractFilterParams(String searchUrl) throws IOException {
        Map<String, String> params = new HashMap<>();
        try {
            URL url = new URL(searchUrl);
            String query = url.getQuery();
            if (query != null) {
                String[] pairs = query.split("&");
                Pattern pattern = Pattern.compile("o_\\d+_(\\d+)_\\d+_\\d+=(\\d+)");
                for (String pair : pairs) {
                    Matcher matcher = pattern.matcher(pair);
                    if (matcher.matches()) {
                        params.put("featureId", matcher.group(1));
                        params.put("optionId", matcher.group(2));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing URL parameters: {}", e.getMessage());
            throw new IOException("Invalid URL format", e);
        }
        return params;
    }

    CarDetails extractDetailedCarInfo(String carLink) {
        try {
            Document doc = Jsoup.connect(base_url + carLink).get();
            
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

            return new CarDetails(base_url + carLink, title + " " + generation, eurPrice, mileage,
                    updateDate, adType, region, author, yearOfFabrication, wheelSide, nrOfSeats, body,
                    nrOfDoors, engineCapacity, horsepower, petrolType, gearsType, tractionType, color);

        } catch (IOException e) {
            System.err.println("Error fetching car details page: " + (base_url + carLink) + " - " + e.getMessage());
            return null;
        }
    }

    private static Integer getIntegerFromSection(Map<String, String> particularities, String element) {
        Integer result = null;
        String text = particularities.get(element);
        if (text != null) {
            try {
                result = Integer.parseInt(text.replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                throw new NumberFormatException(e.getMessage());
            }
        }
        return result;
    }

    private static void extractSection(Document doc, String cssQuery, Map<String, String> generalities) {
        Elements generalitiesItems = doc.select(cssQuery);
        for (Element item : generalitiesItems) {
            Element keyElement = item.selectFirst("span.styles_group__key__uRhnQ");
            Element valueElement = item.selectFirst("span.styles_group__value__XN7OI, a.styles_group__value__XN7OI");
            if (keyElement != null && valueElement != null) {
                generalities.put(keyElement.text(), valueElement.text());
            }
        }
    }

    private static Integer getEurPrice(String eurPriceText) {
        Integer result = null;
        try {
            if (eurPriceText.contains("€")) {
                result = Integer.parseInt(eurPriceText.replaceAll("\\D", ""));
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException(e.getMessage());
        }
        return result;
    }

    private static String getTitle(Document doc) {
        String result = null;
        Element titleElement = doc.selectFirst("h1");
        if (titleElement != null) {
            result = titleElement.text();
        }
        return result;
    }

    private static String getAdInfo(Elements items, String cssQuery) {
        String result = null;
        Element element = items.selectFirst(cssQuery);
        if (element != null) {
            String text = element.text();
            result = text.substring(text.indexOf(":") + 1).trim();
        }
        return result;
    }

    private static String getString(Elements items, String cssQuery) {
        String result = null;
        Element element = items.selectFirst(cssQuery);
        if (element != null) {
            result = element.text();
        }
        return result;
    }

    void printResults(List<CarDetails> finalProducts) {
        if (finalProducts == null || finalProducts.isEmpty()) {
            throw new RuntimeException("Product list is empty or null");
        }

        CarDetails maxEntry = getMaxEntry(finalProducts);

        CarDetails minEntry = getMinEntry(finalProducts);

        double avgPrice = getAvgPrice(finalProducts, 200000, 400000);

        System.out.println("Max price: " + maxEntry.getEurPrice() + " (Link: " + maxEntry.getLink() + ")");
        System.out.println("Min price: " + minEntry.getEurPrice() + " (Link: " + minEntry.getLink() + ")");
        System.out.printf("Average price: %.2f%n", avgPrice);
    }

    private static double getAvgPrice(List<CarDetails> finalProducts, int minMileage, int maxMileage) {
        return finalProducts.stream()
                .filter(c -> c.getMileage() != null && c.getEurPrice() != null && c.getMileage() > minMileage &&
                        c.getMileage() < maxMileage && c.getAdType().equals("Vând"))
                .mapToInt(CarDetails::getEurPrice)
                .average()
                .orElseThrow(() -> new RuntimeException("Cannot compute average - list is empty"));
    }

    private static CarDetails getMinEntry(List<CarDetails> finalProducts) {
        return finalProducts.stream()
                .filter(c -> c.getEurPrice() != null && c.getAdType().equals("Vând"))
                .min(Comparator.comparingInt(CarDetails::getEurPrice))
                .orElseThrow(() -> new RuntimeException("There is no min price"));
    }

    private static CarDetails getMaxEntry(List<CarDetails> finalProducts) {
        return finalProducts.stream()
                .filter(c -> c.getEurPrice() != null && c.getAdType().equals("Vând"))
                .max(Comparator.comparingInt(CarDetails::getEurPrice))
                .orElseThrow(() -> new RuntimeException("There is no max price"));
    }

    private void saveResults(List<CarDetails> finalProducts) {
        if (finalProducts.isEmpty()) {
            logger.info("No products found.");
            return;
        }

        try {
            dbManager.saveCars(finalProducts);
            printResults(finalProducts);
        } catch (SQLException e) {
            logger.error("Failed to save results to database: {}", e.getMessage());
            throw new RuntimeException("Database error", e);
        }
    }
}