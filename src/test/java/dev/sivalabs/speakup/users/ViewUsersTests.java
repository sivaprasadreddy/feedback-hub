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

class ViewUsersTests extends BaseIT {
    @Autowired
    UserRepository userRepository;

    @Test
    void adminCanViewUserDetailsAndFilterByRoleAndStatus() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var suffix = UUID.randomUUID().toString();
        var activeAdminEmail = "active-admin+" + suffix + "@acme.com";
        var inactiveUserEmail = "inactive-user+" + suffix + "@acme.com";
        createUser(adminSession, "Active Admin Candidate", activeAdminEmail, "ROLE_ADMIN");
        createUser(adminSession, "Inactive User Candidate", inactiveUserEmail, "ROLE_USER");
        var inactiveUserId = userRepository
                .findByEmailIgnoreCase(inactiveUserEmail)
                .orElseThrow()
                .getId();
        editUser(adminSession, inactiveUserId, "ROLE_USER", false);

        assertThat(mvc.get().uri("/admin/users").session(adminSession).exchange())
                .hasStatusOk()
                .hasViewName("admin/users")
                .bodyText()
                .contains("Name", "Email", "Role", "Status", "Created")
                .contains("Active Admin Candidate", activeAdminEmail, "ADMIN", "Active")
                .contains("Inactive User Candidate", inactiveUserEmail, "USER", "Inactive");

        assertThat(mvc.get()
                        .uri("/admin/users?role=ROLE_ADMIN")
                        .session(adminSession)
                        .exchange())
                .bodyText()
                .contains("Active Admin Candidate")
                .doesNotContain("Inactive User Candidate");

        assertThat(mvc.get()
                        .uri("/admin/users?active=false")
                        .session(adminSession)
                        .exchange())
                .bodyText()
                .contains("Inactive User Candidate")
                .doesNotContain("Active Admin Candidate");

        assertThat(mvc.get()
                        .uri("/admin/users?role=ROLE_USER&active=false")
                        .session(adminSession)
                        .exchange())
                .bodyText()
                .contains("Inactive User Candidate")
                .doesNotContain("Active Admin Candidate");
    }

    @Test
    void regularAndUnauthenticatedUsersCannotViewUserList() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/users").session(userSession).exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/admin/users").exchange()).hasStatus(HttpStatus.FOUND);
    }

    private void createUser(MockHttpSession session, String name, String email, String role) {
        assertThat(mvc.post()
                        .uri("/admin/users")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", name)
                        .param("email", email)
                        .param("role", role)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private void editUser(MockHttpSession session, Long userId, String role, boolean active) {
        assertThat(mvc.post()
                        .uri("/admin/users/{id}/edit", userId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("role", role)
                        .param("active", Boolean.toString(active))
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }
}
