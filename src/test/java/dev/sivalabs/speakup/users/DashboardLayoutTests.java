package dev.sivalabs.speakup.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.speakup.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class DashboardLayoutTests extends BaseIT {

    @Test
    void homePageIsSeparateFromDashboard() {
        var session = session(login("siva@gmail.com", "secret"));

        var home = mvc.get().uri("/").session(session).exchange();
        assertThat(home)
                .hasStatusOk()
                .hasViewName("index")
                .bodyText()
                .contains("Home")
                .contains("Messages will appear here.")
                .contains("Dashboard");

        var dashboard = mvc.get().uri("/dashboard").session(session).exchange();
        assertThat(dashboard)
                .hasStatusOk()
                .hasViewName("user-dashboard")
                .bodyText()
                .contains("Welcome back")
                .contains("Siva")
                .contains("My Account");
    }

    @Test
    void regularUserSeesAccountMenuOnlyOnDashboard() {
        var dashboard = mvc.get()
                .uri("/dashboard")
                .session(session(login("siva@gmail.com", "secret")))
                .exchange();

        assertThat(dashboard)
                .hasStatusOk()
                .bodyText()
                .contains("My Account")
                .doesNotContain("Manage Users")
                .doesNotContain("Manage Messages");
    }

    @Test
    void adminSeesAdministrationMenuItemsOnDashboard() {
        var dashboard = mvc.get()
                .uri("/dashboard")
                .session(session(login("admin@gmail.com", "secret")))
                .exchange();

        assertThat(dashboard)
                .hasStatusOk()
                .bodyText()
                .contains("My Account")
                .contains("Manage Users")
                .contains("Manage Messages");
    }

    @Test
    void homePageDoesNotShowAdministrationMenu() {
        var home = mvc.get()
                .uri("/")
                .session(session(login("admin@gmail.com", "secret")))
                .exchange();

        assertThat(home)
                .hasStatusOk()
                .bodyText()
                .contains("Dashboard")
                .doesNotContain("Manage Users")
                .doesNotContain("Manage Messages");
    }

    @Test
    void regularUserCanOpenAccountPage() {
        var page = mvc.get()
                .uri("/account")
                .session(session(login("siva@gmail.com", "secret")))
                .exchange();

        assertThat(page).hasStatusOk().hasViewName("account").bodyText().contains("My Account");
    }

    @Test
    void regularUserCannotOpenAdminPages() {
        var session = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/users").session(session).exchange()).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/admin/messages").session(session).exchange()).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanOpenAdministrationPages() {
        var session = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/users").session(session).exchange())
                .hasStatusOk()
                .hasViewName("admin/users")
                .bodyText()
                .contains("Manage Users");
        assertThat(mvc.get().uri("/admin/messages").session(session).exchange())
                .hasStatusOk()
                .hasViewName("admin/messages")
                .bodyText()
                .contains("Manage Messages");
    }

    private MvcTestResult login(String email, String password) {
        var result = mvc.post()
                .uri("/login")
                .param("username", email)
                .param("password", password)
                .with(csrf())
                .exchange();
        assertThat(result).hasStatus(HttpStatus.FOUND);
        return result;
    }

    private static MockHttpSession session(MvcTestResult result) {
        return (MockHttpSession) result.getMvcResult().getRequest().getSession(false);
    }
}
