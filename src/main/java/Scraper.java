import factory.WebDriverFactory;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;


public class Scraper {
    private static final String url = "https://999.md";
    private static final String carBrand = "Renault";
    private static final String carModel = "Megane";
    private static final String carGeneration = "III (2008 - 2016)";
    private static WebDriverFactory factory;

    public Scraper(WebDriverFactory factory) {
        Scraper.factory = factory;
    }

    public void scrape() throws InterruptedException {
        WebDriver driver = factory.createWebDriver();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> driver.quit()));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(4));
        Map<String, List<Integer>> finalProducts = new HashMap<>();

        driver.get(url);
        WebElement transportLink = driver.findElement(By.cssSelector("a[data-category=\"658\"]"));
        transportLink.click();
        WebElement autoturismeLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("ul.styles_subcategory__column__wVUcl li a[data-subcategory='659']")
        ));
        autoturismeLink.click();
        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[data-test-id='filter-search']")
        ));
        searchInput.sendKeys(carBrand);

        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(2000);

        WebElement modelDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'styles_checkbox__item__bOjAW')]//label[text()='" + carModel + "']/ancestor::div[contains(@class,'styles_checkbox__item__bOjAW')]")
        ));

        WebElement generationLabel = modelDiv.findElement(
                By.xpath(".//div[contains(@class,'styles_children__H8mz2')]//label[text()='" + carGeneration + "']")
        );

        if (!generationLabel.isSelected()) {
            js.executeScript("arguments[0].click();", generationLabel);
        }
        try {
            while (true) {
                List<WebElement> cars = new ArrayList<>();
                int maxRetries = 3;
                int attempts = 0;


                while (attempts < maxRetries) {
                    try {
                        cars = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                                By.cssSelector("div.styles_adlist__3YsgA.styles_flex__9wOfD div.AdPhoto_wrapper__gAOIH"))
                        );
                        break;
                    } catch (StaleElementReferenceException e) {
                        System.out.println("StaleElementReferenceException caught. Retrying... (" + (attempts + 1) + "/" + maxRetries + ")");
                        attempts++;
                    } catch (TimeoutException e) {
                        System.out.println("Timeout occurred while waiting for cars to be visible.");
                        driver.quit();
                        break;
                    }
                }

                for (WebElement webElement : cars) {
                    try {
                        WebElement carLink = webElement.findElement(By.cssSelector("div.styles_adlist__3YsgA.styles_flex__9wOfD a.AdPhoto_info__link__OwhY6"));
                        WebElement carName = webElement.findElement(By.cssSelector("div.styles_adlist__3YsgA.styles_flex__9wOfD a.AdPhoto_info__link__OwhY6"));
                        WebElement carPrice = webElement.findElement(By.cssSelector("div.styles_adlist__3YsgA.styles_flex__9wOfD span.AdPrice_price__2L3eA"));
                        String carKm;
                        try {
                            carKm = webElement.findElement(By.cssSelector("div.styles_adlist__3YsgA.styles_flex__9wOfD span.AdPrice_info__LYNmc")).getText();
                        } catch (NoSuchElementException e) {
                            js.executeScript("window.open(arguments[0], '_blank');", url + carLink.getDomAttribute("href"));
                            String originalWindow = driver.getWindowHandle();
                            for (String windowHandle : driver.getWindowHandles()) {
                                if (!windowHandle.equals(originalWindow)) {
                                    driver.switchTo().window(windowHandle);
                                    break;
                                }
                            }
                            WebElement mileageElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector("span.styles_group__key__uRhnQ")
                            ));
                            carKm = (String) js.executeScript("return Array.from(document.querySelectorAll('span.styles_group__key__uRhnQ')).find(el => el.textContent.includes('Rulaj'))?.nextElementSibling?.textContent || '';");
                            if (carKm == null || carKm.isEmpty()) {
                                driver.close();
                                driver.switchTo().window(originalWindow);
                                continue;
                            }
                            driver.close();
                            driver.switchTo().window(originalWindow);
                        }
                        filterResult(carLink.getDomAttribute("href"), carName.getText(), carPrice.getText(), carKm, finalProducts);
                    } catch (StaleElementReferenceException e) {
                        System.out.println("Encountered stale element, skipping to next.");
                    }
                }
                try {
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    Thread.sleep(1000);


                    WebElement nextButton = driver.findElement(By.cssSelector("button.Pagination_pagination__container__buttons__wrapper__icon__next__A22Rc"));
                    if (nextButton.isEnabled() && nextButton.isDisplayed()) {
                        nextButton.click();
                        Thread.sleep(1000);
                    } else {
                        break;
                    }
                } catch (NoSuchElementException | InterruptedException e) {
                    break;
                }
            }
            getResults(finalProducts);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        } finally {
            driver.quit();
        }
    }

    private void filterResult(String carLink, String carName, String carPrice, String carKm, Map<String, List<Integer>> finalProducts) {
        if (!carName.contains(carBrand + " " + carModel) || !carPrice.contains("€")) {
            return;
        }
        int price = Integer.parseInt(carPrice.replaceAll("\\D", ""));
        if (price < 1000 || price > 50000) {
            return;
        }
        int km = Integer.parseInt(carKm.replaceAll("\\D", ""));
        if (km < 1000) {
            return;
        }
        String finalLink = url + carLink;
        finalProducts.put(finalLink, Arrays.asList(price, km));
    }

    private void getResults(Map<String, List<Integer>> finalProducts) {
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

}
