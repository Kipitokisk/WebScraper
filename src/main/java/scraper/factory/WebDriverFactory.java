package scraper.factory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public interface WebDriverFactory {
    WebDriver createWebDriver();
}
