package scraper.factory;

import com.microsoft.playwright.Page;

public interface PlaywrightFactory {
    Page createPage();
    void close();
}
