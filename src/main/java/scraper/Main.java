package scraper;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.database.DatabaseManager;
import scraper.factory.ChromeDriverFactory;
import scraper.factory.FirefoxDriverFactory;
import scraper.factory.WebDriverFactory;
import scraper.logic.Scraper;

import java.net.MalformedURLException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException, MalformedURLException {
        String baseUrl = System.getenv("BASE_URL");
        String carBrand = System.getenv("CAR_BRAND");
        String carModel = System.getenv("CAR_MODEL");
        String carGeneration = System.getenv("CAR_GENERATION");
        String choice = System.getenv("BROWSER_CHOICE");
        String dbUrl = System.getenv("DATASOURCE_URL");
        String dbUser = System.getenv("DATASOURCE_USERNAME");
        String dbPass = System.getenv("DATASOURCE_PASSWORD");
        DatabaseManager databaseManager = new DatabaseManager(dbUrl, dbUser, dbPass);
        Logger scraperLogger = LoggerFactory.getLogger(Scraper.class);
        WebDriverFactory factory;
        WebDriver driver;
        Scraper scraper;
        factory = getWebDriverFactory(choice);
        driver = setupDriver(factory);
        scraper = new Scraper(driver, baseUrl, carBrand, carModel, carGeneration, databaseManager, scraperLogger);
        scraper.scrape();
    }

    private static WebDriverFactory getWebDriverFactory(String choice) {
        WebDriverFactory factory;
        if (choice.equals("Chrome")) {
            factory = new ChromeDriverFactory();
        } else {
            factory = new FirefoxDriverFactory();
        }
        return factory;
    }

    private static WebDriver setupDriver(WebDriverFactory factory) throws MalformedURLException {
        try {
            System.out.println("Waiting for Selenium services to start");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Startup delay interrupted", e);
        }
        WebDriver driver = factory.createWebDriver();
        Runtime.getRuntime().addShutdownHook(new Thread(driver::quit));
        return driver;
    }
}
