package factory;

import com.microsoft.playwright.*;

public class FirefoxPageFactory implements PlaywrightFactory{
    private final Playwright playwright;
    private final Browser browser;

    public FirefoxPageFactory() {
        this.playwright = Playwright.create();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setChannel("firefox");
        this.browser = playwright.firefox().launch(launchOptions);
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
