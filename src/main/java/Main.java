import factory.ChromeDriverFactory;
import factory.FirefoxDriverFactory;
import factory.WebDriverFactory;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        String choice = "Chrome";
        WebDriverFactory factory;
        Scraper scraper;
        if (choice.equals("Chrome")) {
            factory = new ChromeDriverFactory();
        } else {
            factory = new FirefoxDriverFactory();
        }
        scraper = new Scraper(factory);
        scraper.scrape();
    }
}
