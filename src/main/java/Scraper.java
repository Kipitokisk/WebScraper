import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import factory.ChromiumPageFactory;
import factory.PlaywrightFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Scraper {
    private final String base_url;
    private final String car_brand;
    private final String car_model;
    private final String car_generation;
    private Page page;
    private final PlaywrightFactory factory;
    private final DatabaseManager dbManager;

    public Scraper(PlaywrightFactory factory, String base_url, String car_brand, String car_model, String car_generation) {
        this.factory = factory;
        this.base_url = base_url;
        this.car_brand = car_brand;
        this.car_model = car_model;
        this.car_generation = car_generation;
        this.dbManager = new DatabaseManager();
    }

    public void scrape() {
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

    private void navigateToPage() {
        page.navigate(base_url);
        page.getByText("Transport").click();
        page.getByText("Autoturisme").click();
        page.getByPlaceholder("Сăutare").fill(car_brand);
        Locator modelDiv = page.locator("xpath=//div[contains(@class,'styles_checkbox__item__bOjAW')]//label[text()='" + car_model + "']/ancestor::div[contains(@class,'styles_checkbox__item__bOjAW')]");
        modelDiv.locator("xpath=//div[contains(@class,'styles_children__H8mz2')]//label[text()='" + car_generation + "']").click();
    }

    private void processCurrentPage(List<CarDetails> finalProducts) {
        page.waitForSelector("div.styles_adlist__3YsgA.styles_flex__9wOfD");
        String pageSource = page.content();
        Document doc = Jsoup.parse(pageSource);
        Elements carElements = doc.select("div.styles_adlist__3YsgA.styles_flex__9wOfD div.AdPhoto_wrapper__gAOIH");
        for (Element carElement : carElements) {
            try {
                extractCarDetails(carElement, finalProducts);
            } catch (Exception e) {
                System.err.println("Error processing car element: " + e.getMessage());
            }
        }
    }

    private void processAllPages(List<CarDetails> finalProducts) {
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

    private void extractCarDetails(Element carElement, List<CarDetails> finalProducts) {
        Element carLinkElement = carElement.selectFirst("a.AdPhoto_info__link__OwhY6");
        if (carLinkElement == null) return;

        String carLink = carLinkElement.attr("href");
        String carName = carLinkElement.text();

        CarDetails carDetails = extractDetailedCarInfo(carLink, carName);
        if (carDetails != null) {
            finalProducts.add(carDetails);
        }
    }

    private CarDetails extractDetailedCarInfo(String carLink, String carName) {
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
                    throw new NumberFormatException(e.getMessage());
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
                    throw new NumberFormatException(e.getMessage());
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
                    throw new NumberFormatException(e.getMessage());
                }
            }

            Integer nrOfDoors = null;
            String doorsText = particularities.get("Număr uși");
            if (doorsText != null) {
                try {
                    nrOfDoors = Integer.parseInt(doorsText.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                    throw new NumberFormatException(e.getMessage());
                }
            }

            Integer engineCapacity = null;
            String capacityText = particularities.get("Capacitate cilindrică");
            if (capacityText != null) {
                try {
                    engineCapacity = Integer.parseInt(capacityText.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                    throw new NumberFormatException(e.getMessage());
                }
            }

            Integer horsepower = null;
            String hpText = particularities.get("Putere");
            if (hpText != null) {
                try {
                    horsepower = Integer.parseInt(hpText.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                    throw new NumberFormatException(e.getMessage());
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

            return new CarDetails(base_url + carLink, car_brand + " " + car_model + " " + car_generation, eurPrice, mileage,
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
                .filter(c -> c.getEurPrice() != null && c.getAdType().equals("Vând"))
                .max(Comparator.comparingInt(CarDetails::getEurPrice))
                .orElseThrow(() -> new RuntimeException("There is no max price"));

        CarDetails minEntry = finalProducts.stream()
                .filter(c -> c.getEurPrice() != null && c.getAdType().equals("Vând"))
                .min(Comparator.comparingInt(CarDetails::getEurPrice))
                .orElseThrow(() -> new RuntimeException("There is no min price"));

        double avgPrice = finalProducts.stream()
                .filter(c -> c.getMileage() != null && c.getEurPrice() != null && c.getMileage() > 200000 && c.getMileage() < 400000 && c.getAdType().equals("Vând"))
                .mapToInt(CarDetails::getEurPrice)
                .average()
                .orElseThrow(() -> new RuntimeException("Cannot compute average - list is empty"));

        System.out.println("Max price: " + maxEntry.getEurPrice() + " (Link: " + maxEntry.getLink() + ")");
        System.out.println("Min price: " + minEntry.getEurPrice() + " (Link: " + minEntry.getLink() + ")");
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