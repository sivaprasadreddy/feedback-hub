package dev.sivalabs.feedbackhub.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class CreateUserTests extends BaseIT {

    @Test
    void adminCanCreateUserAndSeeThemInTheUserList() {
        var session = session(login("admin@gmail.com", "secret"));
        var email = "john.doe+" + UUID.randomUUID() + "@acme.com";

        var create = mvc.post()
                .uri("/admin/users")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "John Doe")
                .param("email", email)
                .param("role", "ROLE_USER")
                .session(session)
                .with(csrf())
                .exchange();

        assertThat(create).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/admin/users");

        var list = mvc.get().uri("/admin/users").session(session).exchange();
        assertThat(list)
                .hasStatusOk()
                .hasViewName("admin/users")
                .bodyText()
                .contains("John Doe")
                .contains(email)
                .contains("USER")
                .contains("Active");

        var login = login(email, UserService.INITIAL_PASSWORD);
        assertThat(login).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/");
    }

    @Test
    void adminCanCreateAdminAccount() {
        var session = session(login("admin@gmail.com", "secret"));
        var email = "jane.admin+" + UUID.randomUUID() + "@acme.com";

        var create = mvc.post()
                .uri("/admin/users")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Jane Admin")
                .param("email", email)
                .param("role", "ROLE_ADMIN")
                .session(session)
                .with(csrf())
                .exchange();

        assertThat(create).hasStatus(HttpStatus.FOUND);

        var list = mvc.get().uri("/admin/users").session(session).exchange();
        assertThat(list).hasStatusOk().bodyText().contains("Jane Admin").contains("ADMIN");
    }

    @Test
    void duplicateEmailIsRejected() {
        var session = session(login("admin@gmail.com", "secret"));

        var result = mvc.post()
                .uri("/admin/users")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Copy Admin")
                .param("email", "admin@gmail.com")
                .param("role", "ROLE_USER")
                .session(session)
                .with(csrf())
                .exchange();

        assertThat(result).hasStatusOk().hasViewName("admin/users-new");
        assertThat(result).bodyText().contains("Email address is already in use");
    }

    @Test
    void nameEmailAndRoleAreRequired() {
        var session = session(login("admin@gmail.com", "secret"));

        var result = mvc.post()
                .uri("/admin/users")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "")
                .param("email", "")
                .session(session)
                .with(csrf())
                .exchange();

        assertThat(result).hasStatusOk().hasViewName("admin/users-new");
        assertThat(result)
                .bodyText()
                .contains("Name is required")
                .contains("Email is required")
                .contains("Role is required");
    }

    @Test
    void invalidEmailIsRejected() {
        var session = session(login("admin@gmail.com", "secret"));

        var result = mvc.post()
                .uri("/admin/users")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Bad Email")
                .param("email", "not-an-email")
                .param("role", "ROLE_USER")
                .session(session)
                .with(csrf())
                .exchange();

        assertThat(result).hasStatusOk().hasViewName("admin/users-new");
        assertThat(result).bodyText().contains("Email address must be valid");
    }

    @Test
    void invalidRoleIsRejected() {
        var session = session(login("admin@gmail.com", "secret"));

        var result = mvc.post()
                .uri("/admin/users")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Bad Role")
                .param("email", "bad.role@acme.com")
                .param("role", "SUPERUSER")
                .session(session)
                .with(csrf())
                .exchange();

        assertThat(result).hasStatusOk().hasViewName("admin/users-new");
        assertThat(result).bodyText().contains("Role must be ADMIN or USER");
    }

    @Test
    void regularUserCannotCreateAnAccount() {
        var session = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/users/new").session(session).exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post()
                        .uri("/admin/users")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Hacker")
                        .param("email", "hacker@acme.com")
                        .param("role", "ROLE_ADMIN")
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthenticatedRequestsToCreateUserAreRejected() {
        assertThat(mvc.get().uri("/admin/users/new").exchange()).hasStatus(HttpStatus.FOUND);
        assertThat(mvc.get()
                        .uri("/admin/users/new")
                        .exchange()
                        .getMvcResult()
                        .getResponse()
                        .getRedirectedUrl())
                .contains("/login");
    }
}
