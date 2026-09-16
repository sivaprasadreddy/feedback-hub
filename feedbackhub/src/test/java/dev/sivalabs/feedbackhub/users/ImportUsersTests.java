package dev.sivalabs.feedbackhub.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import dev.sivalabs.feedbackhub.BaseIT;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class ImportUsersTests extends BaseIT {
    @Autowired
    UserRepository userRepository;

    @Test
    void adminCanOpenImportUsersPage() {
        var session = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/users/import").session(session).exchange())
                .hasStatusOk()
                .hasViewName("admin/users-import")
                .bodyText()
                .contains("Import Users", "name,email,role", "USER", "ADMIN");
    }

    @Test
    void adminCanImportUsersFromCsv() throws Exception {
        var session = session(login("admin@gmail.com", "secret"));
        var suffix = UUID.randomUUID();
        var userEmail = "csv.user+" + suffix + "@acme.com";
        var adminEmail = "csv.admin+" + suffix + "@acme.com";
        var csv = "name,email,role\nCSV User,%s,USER\nCSV Admin,%s,ROLE_ADMIN\n".formatted(userEmail, adminEmail);

        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        var result = mvc.perform(
                multipart("/admin/users/import").file(file).session(session).with(csrf()));

        assertThat(result).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/admin/users");

        assertThat(mvc.get().uri("/admin/users").session(session).exchange())
                .hasStatusOk()
                .bodyText()
                .contains("2 users imported successfully.", "CSV User", userEmail, "CSV Admin", adminEmail);
        assertThat(login(userEmail, UserService.INITIAL_PASSWORD))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/");
    }

    @Test
    void invalidRowsAreReportedAndNothingIsImported() throws Exception {
        var session = session(login("admin@gmail.com", "secret"));
        var uniqueEmail = "not-imported+" + UUID.randomUUID() + "@acme.com";
        var csv = "name,email,role\nValid User,%s,USER\nBad Email,invalid,USER\nMissing Role,missing@example.com,\n"
                .formatted(uniqueEmail);

        var result = importCsv(session, csv);

        assertThat(result)
                .hasStatusOk()
                .hasViewName("admin/users-import")
                .bodyText()
                .contains("Email address must be valid", "Role must be ADMIN or USER", "No users were imported");
        assertThat(result.getMvcResult().getModelAndView().getModel().get("errors"))
                .asList()
                .extracting("rowNumber")
                .containsExactly(3, 4);
        assertThat(userRepository.findByEmailIgnoreCase(uniqueEmail)).isEmpty();
    }

    @Test
    void wrongHeaderAndDuplicateEmailsAreRejected() throws Exception {
        var session = session(login("admin@gmail.com", "secret"));

        var wrongHeader = importCsv(session, "fullName,email,role\nJane,jane@example.com,USER\n");
        assertThat(wrongHeader).bodyText().contains("Header must be: name,email,role");

        var duplicate = importCsv(
                session, "name,email,role\nAdmin Copy,admin@gmail.com,USER\nOther Copy,ADMIN@gmail.com,ADMIN\n");
        assertThat(duplicate)
                .bodyText()
                .contains("Email address is already in use", "Email is duplicated in the CSV file");
    }

    @Test
    void regularUserCannotImportUsers() throws Exception {
        var session = session(login("siva@gmail.com", "secret"));
        assertThat(mvc.get().uri("/admin/users/import").session(session).exchange())
                .hasStatus(HttpStatus.FORBIDDEN);

        var result = importCsv(session, "name,email,role\nHacker,hacker@example.com,ADMIN\n");
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    private org.springframework.test.web.servlet.assertj.MvcTestResult importCsv(
            org.springframework.mock.web.MockHttpSession session, String csv) throws Exception {
        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        return mvc.perform(
                multipart("/admin/users/import").file(file).session(session).with(csrf()));
    }
}
