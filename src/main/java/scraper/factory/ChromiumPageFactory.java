package scraper.factory;

import com.microsoft.playwright.*;

public class ChromiumPageFactory implements PlaywrightFactory{
    private final Playwright playwright;
    private final Browser browser;

    public ChromiumPageFactory() {
        this.playwright = Playwright.create();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(true);
        this.browser = playwright.chromium().launch(launchOptions);
    }

    @Override
    public Page createPage() {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080));

        context.route("**/*", route -> {
            if (route.request().resourceType().equals("image") ||
                    route.request().resourceType().equals("media")) {
                route.abort();
            } else {
                route.resume();
            }
        });

        Page page = context.newPage();
        return page;
    }

    @Override
    public void close() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
