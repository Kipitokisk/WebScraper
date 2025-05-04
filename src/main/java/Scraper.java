import factory.WebDriverFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

public class Scraper {
    private final String base_url;
    private final String car_brand;
    private final String car_model;
    private final String car_generation;
    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;
    private final WebDriverFactory factory;
    private final DatabaseManager dbManager;

    public Scraper(WebDriverFactory factory, String base_url, String car_brand, String car_model, String car_generation) {
        this.factory = factory;
        this.base_url = base_url;
        this.car_brand = car_brand;
        this.car_model = car_model;
        this.car_generation = car_generation;
        this.dbManager = new DatabaseManager();
    }

    public void scrape() throws InterruptedException {
        driver = setupDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(4));
        js = (JavascriptExecutor) driver;
        Map<String, List<Integer>> finalProducts = new HashMap<>();
        try {
            navigateToSearchPage();
            selectCarModelAndGeneration();
            processAllPages(finalProducts);
            saveResults(finalProducts);
        } finally {
            driver.quit();
        }
    }

    private WebDriver setupDriver() {
        WebDriver driver = factory.createWebDriver();
        Runtime.getRuntime().addShutdownHook(new Thread(driver::quit));
        return driver;
    }

    private void navigateToSearchPage() {
        driver.get(base_url);

        WebElement transportLink = driver.findElement(By.cssSelector("a[data-category=\"658\"]"));
        js.executeScript("arguments[0].click();", transportLink);

        WebElement autoturismeLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("ul.styles_subcategory__column__wVUcl li a[data-subcategory='659']")
        ));
        js.executeScript("arguments[0].click();", autoturismeLink);

        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[data-test-id='filter-search']")
        ));
        searchInput.sendKeys(car_brand);
    }

    private void selectCarModelAndGeneration() {
        WebElement modelDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'styles_checkbox__item__bOjAW')]//label[text()='" + car_model + "']/ancestor::div[contains(@class,'styles_checkbox__item__bOjAW')]")
        ));

        WebElement generationLabel = modelDiv.findElement(
                By.xpath(".//div[contains(@class,'styles_children__H8mz2')]//label[text()='" + car_generation + "']")
        );

        if (!generationLabel.isSelected()) {
            js.executeScript("arguments[0].click();", generationLabel);
        }
    }

    private void processAllPages(Map<String, List<Integer>> finalProducts) throws InterruptedException {
        while (true) {
            processCurrentPage(finalProducts);

            try {
                Thread.sleep(1000);

                WebElement nextButton = driver.findElement(By.cssSelector("button.Pagination_pagination__container__buttons__wrapper__icon__next__A22Rc"));
                if (nextButton.isEnabled() && nextButton.isDisplayed()) {
                    js.executeScript("arguments[0].click();", nextButton);
                    Thread.sleep(1000);
                } else {
                    break;
                }
            } catch (NoSuchElementException | InterruptedException e) {
                break;
            }
        }
    }

    private void processCurrentPage(Map<String, List<Integer>> finalProducts) {
        int maxRetries = 3;
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                String pageSource = driver.getPageSource();
                Document doc = Jsoup.parse(pageSource);
                Elements carElements = doc.select("div.styles_adlist__3YsgA.styles_flex__9wOfD div.AdPhoto_wrapper__gAOIH");

                for (Element carElement : carElements) {
                    try {
                        extractCarDetails(carElement, finalProducts);
                    } catch (Exception e) {
                        System.err.println("Error processing car element: " + e.getMessage());
                    }
                }
                break;
            } catch (Exception e) {
                System.err.println("Error processing page, retrying... (" + (attempts + 1) + "/" + maxRetries + ")");
                attempts++;
            }
        }
    }

    private void extractCarDetails(Element carElement, Map<String, List<Integer>> finalProducts) throws InterruptedException {
        Element carLinkElement = carElement.selectFirst("a.AdPhoto_info__link__OwhY6");
        Element carPriceElement = carElement.selectFirst("span.AdPrice_price__2L3eA");
        if (carLinkElement == null || carPriceElement == null) return;

        String carLink = carLinkElement.attr("href");
        String carName = carLinkElement.text();
        String carPrice = carPriceElement.text();

        String carKm = extractMileage(driver, wait, js, carElement, carLink);
        if (carKm == null || carKm.isEmpty()) return;

        filterResult(carLink, carName, carPrice, carKm, finalProducts);
    }

    private String extractMileage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,  Element carElement, String carLink) throws InterruptedException {
        Element mileageElement = carElement.selectFirst("span.AdPrice_info__LYNmc");
        if (mileageElement != null) {
            return mileageElement.text();
        }

        js.executeScript("window.open(arguments[0], '_blank');", base_url + carLink);
        String originalWindow = driver.getWindowHandle();

        for (String windowHandle : driver.getWindowHandles()) {
            if (!windowHandle.equals(originalWindow)) {
                driver.switchTo().window(windowHandle);
                break;
            }
        }

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("span.styles_group__key__uRhnQ")
            ));
            return (String) js.executeScript(
                    "return Array.from(document.querySelectorAll('span.styles_group__key__uRhnQ')).find(el => el.textContent.includes('Rulaj'))?.nextElementSibling?.textContent || '';"
            );
        } catch (TimeoutException e) {
            return null;
        } finally {
            driver.close();
            driver.switchTo().window(originalWindow);
        }
    }

    private void filterResult(String carLink, String carName, String carPrice, String carKm, Map<String, List<Integer>> finalProducts) {
        if (!carName.contains(car_brand + " " + car_model) || !carPrice.contains("€")) {
            return;
        }

        int price;
        try {
            price = Integer.parseInt(carPrice.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return;
        }
        if (price < 1000 || price > 50000) {
            return;
        }

        int km;
        try {
            km = Integer.parseInt(carKm.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return;
        }
        if (km < 1000) {
            return;
        }

        String finalLink = base_url + carLink;
        finalProducts.put(finalLink, Arrays.asList(price, km));
    }

    private void printResults(Map<String, List<Integer>> finalProducts) {
        if (finalProducts == null || finalProducts.isEmpty()) {
            throw new RuntimeException("Product map is empty or null");
        }

        Map.Entry<String, List<Integer>> maxEntry = finalProducts.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .max(Comparator.comparingInt(e -> e.getValue().get(0)))
                .orElseThrow(() -> new RuntimeException("There is no max price"));

        Map.Entry<String, List<Integer>> minEntry = finalProducts.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .min(Comparator.comparingInt(e -> e.getValue().get(0)))
                .orElseThrow(() -> new RuntimeException("There is no min price"));

        double avgPrice = finalProducts.values().stream()
                .filter(list -> list != null && !list.isEmpty() && list.get(1) > 200000 && list.get(1) < 400000)
                .mapToInt(list -> list.get(0))
                .average()
                .orElseThrow(() -> new RuntimeException("Cannot compute average - list is empty"));

        System.out.println("Max price: " + maxEntry.getValue().get(0) + " (Link: " + maxEntry.getKey() + ")");
        System.out.println("Min price: " + minEntry.getValue().get(0) + " (Link: " + minEntry.getKey() + ")");
        System.out.printf("Average price: %.2f%n", avgPrice);
    }

    private void saveResults(Map<String, List<Integer>> finalProducts) {
        if (finalProducts.isEmpty()) {
            System.out.println("No products found.");
            return;
        }

        try {
            dbManager.saveCars(finalProducts);
            printResults(finalProducts);
        } catch (SQLException e) {
            System.err.println("Failed to save results to database: " + e.getMessage());
            throw new RuntimeException("Database error", e);
        }
    }

    private static class DatabaseManager {
        private static final String DB_URL = "jdbc:postgresql://localhost:5432/scraper_db";
        private static final String DB_USER = "postgres";
        private static final String DB_PASSWORD = "pass";

        public void saveCars(Map<String, List<Integer>> finalProducts) throws SQLException {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO cars (link, price, mileage) VALUES (?, ?, ?) ON CONFLICT (link) DO NOTHING";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, List<Integer>> entry : finalProducts.entrySet()) {
                        String link = entry.getKey();
                        List<Integer> values = entry.getValue();
                        if (values == null || values.size() < 2) continue;

                        if (link.contains("?clickToken")) {
                            int queryIndex = link.indexOf('?');
                            if (queryIndex != -1) {
                                link = link.substring(0, queryIndex);
                            }
                        }

                        stmt.setString(1, link);
                        stmt.setInt(2, values.get(0)); // price
                        stmt.setInt(3, values.get(1)); // mileage
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            }
        }
    }
}

