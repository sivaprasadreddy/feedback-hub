package dev.sivalabs.speakup.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.speakup.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class AuthenticationTests extends BaseIT {

    @Test
    void activeUserWithValidCredentialsCanAuthenticate() {
        var result = login("siva@gmail.com", "secret");

        assertThat(result).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/");

        var user = authenticatedUser(result);
        assertThat(user.getId()).isEqualTo(2L);
        assertThat(user.getUsername()).isEqualTo("siva@gmail.com");
        assertThat(user.getName()).isEqualTo("Siva");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user.isEnabled()).isTrue();

        var home = mvc.get().uri("/").session(session(result)).exchange();
        assertThat(home)
                .hasStatusOk()
                .hasViewName("index")
                .bodyText()
                .contains("Siva")
                .contains("Home")
                .contains("Dashboard")
                .contains("Messages will appear here.")
                .doesNotContain("Manage Users")
                .doesNotContain("Manage Messages");
    }

    @Test
    void adminAuthenticationIncludesIdentityAndRole() {
        var result = login("admin@gmail.com", "secret");

        assertThat(result).hasStatus(HttpStatus.FOUND);

        var user = authenticatedUser(result);
        assertThat(user.getUsername()).isEqualTo("admin@gmail.com");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void unknownEmailIsRejectedWithoutRevealingAccountExistence() {
        var result = login("missing@gmail.com", "secret");

        assertThat(result).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/login?error");
        assertThat(authenticatedUserOrNull(result)).isNull();

        var loginPage = mvc.get().uri("/login?error").exchange();
        assertThat(loginPage)
                .hasStatusOk()
                .bodyText()
                .contains("Invalid email or password.")
                .doesNotContain("missing@gmail.com")
                .doesNotContain("not found");
    }

    @Test
    void wrongPasswordIsRejectedWithoutRevealingAccountExistence() {
        var result = login("siva@gmail.com", "wrong-password");

        assertThat(result).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/login?error");
        assertThat(authenticatedUserOrNull(result)).isNull();

        var loginPage = mvc.get().uri("/login?error").exchange();
        assertThat(loginPage)
                .hasStatusOk()
                .bodyText()
                .contains("Invalid email or password.")
                .doesNotContain("siva@gmail.com");
    }

    @Test
    void inactiveUserCannotAuthenticate() {
        var result = login("prasad@gmail.com", "secret");

        assertThat(result).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/login?error");
        assertThat(authenticatedUserOrNull(result)).isNull();

        var loginPage = mvc.get().uri("/login?error").exchange();
        assertThat(loginPage).hasStatusOk().bodyText().contains("Invalid email or password.");

        var home = mvc.get().uri("/").session(session(result)).exchange();
        assertThat(home).hasStatus(HttpStatus.FOUND);
        assertThat(home.getMvcResult().getResponse().getRedirectedUrl()).contains("/login");
    }

    @Test
    void unauthenticatedRequestsToProtectedFeaturesAreRejected() {
        assertRedirectsToLogin(mvc.get().uri("/").exchange());
        assertRedirectsToLogin(mvc.get().uri("/admin/users").exchange());
    }

    private MvcTestResult login(String email, String password) {
        return mvc.post()
                .uri("/login")
                .param("username", email)
                .param("password", password)
                .with(csrf())
                .exchange();
    }

    private static MockHttpSession session(MvcTestResult result) {
        return (MockHttpSession) result.getMvcResult().getRequest().getSession(false);
    }

    private static SecurityUser authenticatedUser(MvcTestResult result) {
        var user = authenticatedUserOrNull(result);
        assertThat(user).isNotNull();
        return user;
    }

    private static SecurityUser authenticatedUserOrNull(MvcTestResult result) {
        var session = result.getMvcResult().getRequest().getSession(false);
        if (session == null) {
            return null;
        }
        var context = (SecurityContext)
                session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (context == null || context.getAuthentication() == null) {
            return null;
        }
        if (context.getAuthentication().getPrincipal() instanceof SecurityUser user) {
            return user;
        }
        return null;
    }

    private static void assertRedirectsToLogin(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.FOUND);
        assertThat(result.getMvcResult().getResponse().getRedirectedUrl()).contains("/login");
    }
}
