package scraper.factory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

public class FirefoxDriverFactory implements WebDriverFactory{
    @Override
    public WebDriver createWebDriver() throws MalformedURLException {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("-headless");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--blink-settings=imagesEnabled=false");

        URL seleniumServerUrl = new URL(System.getenv("SELENIUM_FIREFOX_URL"));

        return new RemoteWebDriver(seleniumServerUrl, options);
    }
}
