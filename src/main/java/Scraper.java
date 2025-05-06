import factory.WebDriverFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
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
        List<CarDetails> finalProducts = new ArrayList<>();
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

    private void processAllPages(List<CarDetails> finalProducts) throws InterruptedException {
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

    private void processCurrentPage(List<CarDetails> finalProducts) {
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

    private void extractCarDetails(Element carElement, List<CarDetails> finalProducts) throws IOException {
        Element carLinkElement = carElement.selectFirst("a.AdPhoto_info__link__OwhY6");
        if (carLinkElement == null) return;

        String carLink = carLinkElement.attr("href");
        String carName = carLinkElement.text();

        CarDetails carDetails = extractDetailedCarInfo(carLink, carName);
        if (carDetails != null) {
            finalProducts.add(carDetails);
        }
    }

    private CarDetails extractDetailedCarInfo(String carLink, String carName) throws IOException {
        try {
            Document doc = Jsoup.connect(base_url + carLink).get();
            Elements items = doc.select("div.styles_aside__0m8KW");

            String updateDate = null;
            Element updateDateElement = items.selectFirst("p.styles_date__voWnk");
            if (updateDateElement != null) {
                String text = updateDateElement.text();
                updateDate = text.substring(text.indexOf(":") + 1).trim();
            }

            String adType = null;
            Element adTypeElement = items.selectFirst("p.styles_type___J9Dy");
            if (adTypeElement != null) {
                String text = adTypeElement.text();
                adType = text.substring(text.indexOf(":") + 1).trim();
            }

            Integer eurPrice = null;
            Element eurPriceElement = items.selectFirst("span.styles_sidebar__main__DaXQC");
            if (eurPriceElement != null) {
                String eurPriceString = eurPriceElement.text();
                try {
                    eurPrice = Integer.parseInt(eurPriceString.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                    eurPrice = null;
                }
            }

            String region = null;
            Element regionElement = items.selectFirst("span.styles_address__text__duvKg");
            if (regionElement != null) {
                region = regionElement.text();
            }

            String author = null;
            Element authorElement = doc.selectFirst("a.styles_owner__login__VKE71");
            if (authorElement != null) {
                author = authorElement.text().trim();
            }

            Map<String, String> particularities = new HashMap<>();
            Elements particularityItems = doc.select("div.styles_features__right__Sn6fV > div.styles_group__aota8 > ul > li");
            for (Element item : particularityItems) {
                Element keyElement = item.selectFirst("span.styles_group__key__uRhnQ");
                Element valueElement = item.selectFirst("span.styles_group__value__XN7OI");
                if (keyElement != null && valueElement != null) {
                    particularities.put(keyElement.text(), valueElement.text());
                }
            }

            Integer yearOfFabrication = null;
            String yearText = particularities.get("An de fabricație");
            if (yearText != null) {
                try {
                    yearOfFabrication = Integer.parseInt(yearText.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                }
            }

            String wheelSide = particularities.get("Volan");
            String body = particularities.get("Tip caroserie");
            String color = particularities.get("Culoare");

            Integer nrOfSeats = null;
            String seatsText = particularities.get("Număr de locuri");
            if (seatsText != null) {
                try {
                    nrOfSeats = Integer.parseInt(seatsText.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                }
            }

            Integer nrOfDoors = null;
            String doorsText = particularities.get("Număr uși");
            if (doorsText != null) {
                try {
                    nrOfDoors = Integer.parseInt(doorsText.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                }
            }

            Integer engineCapacity = null;
            String capacityText = particularities.get("Capacitate cilindrică");
            if (capacityText != null) {
                try {
                    engineCapacity = Integer.parseInt(capacityText.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                }
            }

            Integer horsepower = null;
            String hpText = particularities.get("Putere");
            if (hpText != null) {
                try {
                    horsepower = Integer.parseInt(hpText.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                }
            }

            String petrolType = particularities.get("Tip combustibil");
            String gearsType = particularities.get("Cutie de viteze");
            String tractionType = particularities.get("Tip tracțiune");

            Integer mileage = null;
            String mileageText = particularities.get("Rulaj");
            if (mileageText != null) {
                try {
                    mileage = Integer.parseInt(mileageText.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            if (!carName.contains(car_brand + " " + car_model) ||
                    (eurPrice != null && (eurPrice < 100 || eurPrice > 70000)) ||
                    (mileage != null && (mileage < 100 || mileage == 111 ||mileage == 1111 || mileage == 11111 || mileage == 111111 ||
                            mileage == 77777 || mileage == 777777 || mileage == 12345 || mileage == 123456))) {
                return null;
            }

            return new CarDetails(base_url + carLink, car_brand + car_model + car_generation, eurPrice, mileage,
                    updateDate, adType, region, author, yearOfFabrication, wheelSide, nrOfSeats, body,
                    nrOfDoors, engineCapacity, horsepower, petrolType, gearsType, tractionType, color);

        } catch (IOException e) {
            System.err.println("Error fetching car details page: " + (base_url + carLink) + " - " + e.getMessage());
            return null;
        }
    }

    private void printResults(List<CarDetails> finalProducts) {
        if (finalProducts == null || finalProducts.isEmpty()) {
            throw new RuntimeException("Product list is empty or null");
        }

        CarDetails maxEntry = finalProducts.stream()
                .filter(c -> c.eurPrice != null && c.adType.equals("Vând"))
                .max(Comparator.comparingInt(c -> c.eurPrice))
                .orElseThrow(() -> new RuntimeException("There is no max price"));

        CarDetails minEntry = finalProducts.stream()
                .filter(c -> c.eurPrice != null && c.adType.equals("Vând"))
                .min(Comparator.comparingInt(c -> c.eurPrice))
                .orElseThrow(() -> new RuntimeException("There is no min price"));

        double avgPrice = finalProducts.stream()
                .filter(c -> c.mileage != null && c.eurPrice != null && c.mileage > 200000 && c.mileage < 400000 && c.adType.equals("Vând"))
                .mapToInt(c -> c.eurPrice)
                .average()
                .orElseThrow(() -> new RuntimeException("Cannot compute average - list is empty"));

        System.out.println("Max price: " + maxEntry.eurPrice + " (Link: " + maxEntry.link + ")");
        System.out.println("Min price: " + minEntry.eurPrice + " (Link: " + minEntry.link + ")");
        System.out.printf("Average price: %.2f%n", avgPrice);
    }

    private void saveResults(List<CarDetails> finalProducts) {
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
}