import factory.ChromiumPageFactory;
import factory.FirefoxPageFactory;
import factory.PlaywrightFactory;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        String base_url = "https://999.md";
        String car_brand = "Renault";
        String car_model = "Megane";
        String car_generation = "III (2008 - 2016)";
        String choice = "Chrome";
        PlaywrightFactory factory;
        Scraper scraper;
        if (choice.equals("Chrome")) {
            factory = new ChromiumPageFactory();
        } else factory = new FirefoxPageFactory();

        scraper = new Scraper(factory, base_url, car_brand, car_model, car_generation);
        scraper.scrape();
    }
}
