package dev.sivalabs.feedbackhub;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class LoginAndPostMessageTests {
    private static final String BASE_URL = System.getenv().getOrDefault("FEEDBACK_HUB_URL", "http://localhost:8080");
    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright
                .chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(2000));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createPage() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @ParameterizedTest(name = "{0} can log in and post a message")
    @CsvFileSource(resources = "/users.csv", numLinesToSkip = 1)
    void userCanLogInAndPostMessage(String email, String password) {
        String message = "Playwright feedback from " + email + " " + UUID.randomUUID();

        page.navigate(BASE_URL + "/login");
        page.getByLabel("Email *").fill(email);
        page.getByLabel("Password *").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in"))
                .click();

        assertThat(page).hasURL(BASE_URL + "/");
        page.getByLabel("Message").fill(message);
        page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("Myself"))
                .check();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Post message"))
                .click();

        assertThat(page).hasURL(BASE_URL + "/");
        assertThat(page.getByText(message, new Page.GetByTextOptions().setExact(true)))
                .isVisible();
    }
}
