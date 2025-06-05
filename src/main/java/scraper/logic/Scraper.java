package scraper.logic;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import scraper.database.DatabaseManager;
import scraper.factory.PlaywrightFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import scraper.model.CarDetails;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Scraper {
    private final Logger logger;
    private final String baseUrl;
    private final String carBrand;
    private final String carModel;
    private final String carGeneration;
    private Page page;
    private final PlaywrightFactory factory;
    private final DatabaseManager dbManager;

    public Scraper(PlaywrightFactory factory, String baseUrl, String carBrand, String carModel, String carGeneration, Logger logger, DatabaseManager databaseManager) {
        this.factory = factory;
        this.baseUrl = baseUrl;
        this.carBrand = carBrand;
        this.carModel = carModel;
        this.carGeneration = carGeneration;
        this.logger = logger;
        this.dbManager = databaseManager;
    }

    public void scrape() throws SQLException {
        page = factory.createPage();
        List<CarDetails> finalProducts = new ArrayList<>();
        try {
            navigateToPage();
            processAllPages(finalProducts);
            saveResults(finalProducts);

        } finally {
            if (page != null) {
                page.close();
            }
            factory.close();
        }
    }

    void navigateToPage() {
        page.navigate(baseUrl);
        page.getByText("Transport").click();
        page.getByText("Autoturisme").click();
        page.getByPlaceholder("Сăutare").fill(carBrand);
        Locator modelDiv = page
                .locator("xpath=//div[contains(@class,'styles_checkbox__item__bOjAW')]//label[text()='" + carModel + "']/ancestor::div[contains(@class,'styles_checkbox__item__bOjAW')]");
        modelDiv
                .locator("xpath=//div[contains(@class,'styles_children__H8mz2')]//label[text()='" + carGeneration + "']").click();
    }

    void processCurrentPage(List<CarDetails> finalProducts) {
        page.waitForSelector("div.styles_adlist__3YsgA.styles_flex__9wOfD");
        String pageSource = page.content();
        Document doc = Jsoup.parse(pageSource);
        Elements carElements = doc
                .select("div.styles_adlist__3YsgA.styles_flex__9wOfD div.AdPhoto_wrapper__gAOIH");
        for (Element carElement : carElements) {
            try {
                extractCarDetails(carElement, finalProducts);
            } catch (Exception e) {
                logger.error("Error processing car element, skipping to next");
            }
        }
    }

    void processAllPages(List<CarDetails> finalProducts) {
        while (true) {
            processCurrentPage(finalProducts);
            Locator nextButton = page.getByText("›");
            if (nextButton.isEnabled()) {
                nextButton.click();
            } else {
                break;
            }
        }
    }

    void extractCarDetails(Element carElement, List<CarDetails> finalProducts) {
        Element carLinkElement = carElement.selectFirst("a.AdPhoto_info__link__OwhY6");
        if (carLinkElement == null) return;

        String carLink = carLinkElement.attr("href");

        CarDetails carDetails = extractDetailedCarInfo(carLink);
        if (carDetails != null) {
            finalProducts.add(carDetails);
        }
    }

    CarDetails extractDetailedCarInfo(String carLink) {
        try {
            Document doc = Jsoup.connect(baseUrl + carLink).get();

            String title = getTitle(doc);
            if (!title.contains(carBrand + " " + carModel)) {
                return null;
            }

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
                    .color(color).build();

        } catch (IOException e) {
            logger.error("Error fetching car details page: {}{} - {}",baseUrl, carLink, e.getMessage());
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
            Element valueElement = item
                    .selectFirst("span.styles_group__value__XN7OI, a.styles_group__value__XN7OI");
            if (keyElement != null && valueElement != null) {
                map.put(keyElement.text(), valueElement.text());
            }
        }
    }

    Integer getEurPrice(String eurPriceText) {
        Integer result = null;
        try {
            if (eurPriceText.contains("€")) {
                result = Integer.parseInt(eurPriceText.replaceAll("\\D", ""));
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Price not valid, skipping car");
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
                .filter(c -> c.getMileage() != null && c.getEurPrice() != null &&
                        c.getMileage() > minMileage &&
                        c.getMileage() < maxMileage &&
                        c.getAdType().equals("Vând"))
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

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }
}