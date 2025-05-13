import factory.ChromeDriverFactory;
import factory.FirefoxDriverFactory;
import factory.WebDriverFactory;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        String base_url = "https://999.md";
        String car_brand = "Renault";
        String car_model = "Megane";
        String car_generation = "III (2008 - 2016)";
        String choice = "Firefox";
        WebDriverFactory factory;
        Scraper scraper;
        if (choice.equals("Chrome")) {
            factory = new ChromeDriverFactory();
        } else {
            factory = new FirefoxDriverFactory();
        }
        scraper = new Scraper(factory, base_url, car_brand, car_model, car_generation);
        scraper.scrape();
    }
}
