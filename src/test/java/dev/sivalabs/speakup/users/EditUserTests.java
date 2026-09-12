package dev.sivalabs.speakup.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.speakup.BaseIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class EditUserTests extends BaseIT {
    @Autowired
    UserRepository userRepository;

    @Test
    void adminCanEditRoleAndStatusTogether() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var email = "edit-user+" + UUID.randomUUID() + "@acme.com";
        createUser(adminSession, email);
        var userId = findUserId(email);

        var result = editUser(adminSession, userId, "ROLE_ADMIN", false);

        assertThat(result).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/admin/users");
        var editedUser = userRepository.findById(userId).orElseThrow();
        assertThat(editedUser.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(editedUser.isActive()).isFalse();
        assertThat(login(email, UserService.INITIAL_PASSWORD))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login?error");
    }

    @Test
    void deactivationRejectsAnExistingAuthenticatedSessionAndReactivationRestoresAccess() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var email = "status-change+" + UUID.randomUUID() + "@acme.com";
        createUser(adminSession, email);
        var userId = findUserId(email);
        var userSession = session(login(email, UserService.INITIAL_PASSWORD));

        editUser(adminSession, userId, "ROLE_USER", false);

        assertThat(mvc.get().uri("/").session(userSession).exchange()).hasStatus(HttpStatus.FOUND);
        assertThat(login(email, UserService.INITIAL_PASSWORD))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login?error");

        editUser(adminSession, userId, "ROLE_USER", true);

        assertThat(login(email, UserService.INITIAL_PASSWORD))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/");
    }

    @Test
    void changedRoleIsAppliedToAnExistingAuthenticatedSession() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var email = "role-change+" + UUID.randomUUID() + "@acme.com";
        createUser(adminSession, email);
        var userId = findUserId(email);
        var userSession = session(login(email, UserService.INITIAL_PASSWORD));

        editUser(adminSession, userId, "ROLE_ADMIN", true);

        assertThat(mvc.get().uri("/admin/users").session(userSession).exchange())
                .hasStatusOk();
    }

    @Test
    void invalidRoleIsRejectedWithoutChangingTheUser() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var email = "invalid-role+" + UUID.randomUUID() + "@acme.com";
        createUser(adminSession, email);
        var userId = findUserId(email);

        var result = editUser(adminSession, userId, "SUPERUSER", false);

        assertThat(result).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/admin/users");
        var unchangedUser = userRepository.findById(userId).orElseThrow();
        assertThat(unchangedUser.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(unchangedUser.isActive()).isTrue();
    }

    @Test
    void adminCannotEditOwnAccount() {
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(editUser(adminSession, 1L, "ROLE_USER", false)).hasStatusOk().hasViewName("error/403");
    }

    @Test
    void regularAndUnauthenticatedUsersCannotEditAccounts() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(editUser(userSession, 1L, "ROLE_USER", true)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post()
                        .uri("/admin/users/1/edit")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("role", "ROLE_USER")
                        .param("active", "true")
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private MvcTestResult editUser(MockHttpSession adminSession, Long userId, String role, boolean active) {
        return mvc.post()
                .uri("/admin/users/{id}/edit", userId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("role", role)
                .param("active", Boolean.toString(active))
                .session(adminSession)
                .with(csrf())
                .exchange();
    }

    private void createUser(MockHttpSession adminSession, String email) {
        assertThat(mvc.post()
                        .uri("/admin/users")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Edit Candidate")
                        .param("email", email)
                        .param("role", "ROLE_USER")
                        .session(adminSession)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private Long findUserId(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
    }
}
