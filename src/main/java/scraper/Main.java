package scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.database.DatabaseManager;
import scraper.factory.ChromiumPageFactory;
import scraper.factory.FirefoxPageFactory;
import scraper.factory.PlaywrightFactory;
import scraper.logic.Scraper;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
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
        PlaywrightFactory factory;
        Scraper scraper;
        if (choice.equals("Chrome")) {
            factory = new ChromiumPageFactory();
        } else factory = new FirefoxPageFactory();

        scraper = new Scraper(factory, baseUrl, carBrand, carModel, carGeneration, scraperLogger, databaseManager);
        scraper.scrape();
    }
}
