package factory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeDriverFactory implements WebDriverFactory{

    @Override
    public WebDriver createWebDriver() {
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless=new");
//        options.addArguments("--window-size=1920,1080");
//        options.addArguments("--disable-gpu");
        options.addArguments("--blink-settings=imagesEnabled=false");
        return new ChromeDriver(options);
    }
}
