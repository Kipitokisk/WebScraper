package scraper;

import factory.ChromiumPageFactory;
import factory.FirefoxPageFactory;
import factory.PlaywrightFactory;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        String baseUrl = "https://999.md";
        String carBrand = "Renault";
        String carModel = "Megane";
        String carGeneration = "III (2008 - 2016)";
        String choice = "Chrome";
        PlaywrightFactory factory;
        Scraper scraper;
        if (choice.equals("Chrome")) {
            factory = new ChromiumPageFactory();
        } else factory = new FirefoxPageFactory();

        scraper = new Scraper(factory, baseUrl, carBrand, carModel, carGeneration);
        scraper.scrape();
    }
}
