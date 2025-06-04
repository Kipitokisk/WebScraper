package scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.database.DatabaseManager;
import scraper.logic.Scraper;

import java.net.http.HttpClient;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String baseUrl = System.getenv("BASE_URL");
        String searchUrl = System.getenv("SEARCH_URL");
        String dbUrl = System.getenv("DATASOURCE_URL");
        String dbUser = System.getenv("DATASOURCE_USERNAME");
        String dbPass = System.getenv("DATASOURCE_PASSWORD");
        HttpClient client = HttpClient.newHttpClient();
        Logger scraperLogger = LoggerFactory.getLogger(Scraper.class);

        try {
            long startTime = System.nanoTime();
            DatabaseManager databaseManager = new DatabaseManager(dbUrl, dbUser, dbPass);
            Scraper scraper = new Scraper(baseUrl, searchUrl, databaseManager, scraperLogger, client);
            scraper.scrape();
            long endTime = System.nanoTime() - startTime;
            logger.info("Scraping completed successfully");
            logger.info("Time in seconds: {}", endTime/1000000000L);
        } catch (Exception e) {
            logger.error("Error running scraper", e);
        }
    }
}