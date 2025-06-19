package scraper;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.database.DatabaseManager;
import scraper.logic.Scraper;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    static Dotenv dotenv = Dotenv.load();
    static int maxThreads = 25;

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Main.class);
        Dotenv dotenv = Dotenv.load();
        String baseUrl = dotenv.get("BASE_URL");
        String searchUrl = dotenv.get("SEARCH_URL");
        String dbUrl = System.getenv("DATASOURCE_URL");
        String dbUser = System.getenv("DATASOURCE_USERNAME");
        String dbPass = System.getenv("DATASOURCE_PASSWORD");
        HttpClient client = HttpClient.newHttpClient();
        Logger scraperLogger = LoggerFactory.getLogger(Scraper.class);

        try {
            DatabaseManager databaseManager = new DatabaseManager(dbUrl, dbUser, dbPass);
            int threads = getNrOfThreads();
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            Scraper scraper = new Scraper(baseUrl, searchUrl, databaseManager, scraperLogger, client, executor);


            while (true) {
                logger.info("Started scheduled scraping.");
                long startTime = System.nanoTime();
                scraper.scrape();
                long endTime = System.nanoTime() - startTime;
                logger.info("Scraping completed successfully");
                logger.info("Time in seconds: {}", endTime/1000000000L);
                Thread.sleep(2 * 60 * 60 * 1000);
            }
        } catch (Exception e) {
            logger.error("Error running scraper", e);
        }
    }

    static int getNrOfThreads() {
        int threadCount = Integer.parseInt(dotenv.get("THREAD_COUNT"));
        if (threadCount != 0) {
            return Math.min(threadCount, maxThreads);
        }
        return maxThreads;
    }
}