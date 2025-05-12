import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String baseUrl = "https://999.md";
        String searchUrl = "https://999.md/ro/list/transport/cars?appl=1&ef=16,1,6,2200&eo=12885,12900,12912,139,35538&aof=20&o_1_2095_8_98=36188";

        try {
            long startTime = System.nanoTime();
            Scraper scraper = new Scraper(baseUrl, searchUrl);
            scraper.scrape();
            long endTime = System.nanoTime() - startTime;
            logger.info("Scraping completed successfully");
            System.out.println(endTime);
        } catch (Exception e) {
            logger.error("Error running scraper", e);
        }
    }
}