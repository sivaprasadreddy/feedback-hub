package dev.sivalabs.feedbackhub.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.users.domain.UserService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class ImportUsersTests extends BaseIT {

    @Autowired
    private UserService userService;

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
    void adminCanImportUsersFromCsv() {
        var session = session(login("admin@gmail.com", "secret"));
        var suffix = UUID.randomUUID();
        var userEmail = "csv.user+" + suffix + "@acme.com";
        var adminEmail = "csv.admin+" + suffix + "@acme.com";
        var csv = "name,email,role%nCSV User,%s,USER%nCSV Admin,%s,ROLE_ADMIN%n".formatted(userEmail, adminEmail);

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
    void invalidRowsAreReportedAndNothingIsImported() {
        var session = session(login("admin@gmail.com", "secret"));
        var uniqueEmail = "not-imported+" + UUID.randomUUID() + "@acme.com";
        var csv = "name,email,role%nValid User,%s,USER%nBad Email,invalid,USER%nMissing Role,missing@example.com,%n"
                .formatted(uniqueEmail);

        var result = importCsv(session, csv);

        assertThat(result)
                .hasStatusOk()
                .hasViewName("admin/users-import")
                .bodyText()
                .contains("Email address must be valid", "Role must be ADMIN or USER", "No users were imported");
        assertThat(result.getMvcResult().getModelAndView()).isNotNull();
        assertThat(result.getMvcResult().getModelAndView().getModel().get("errors"))
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .extracting("rowNumber")
                .containsExactly(3, 4);
        assertThat(userService.findByEmail(uniqueEmail)).isEmpty();
    }

    @Test
    void wrongHeaderAndDuplicateEmailsAreRejected() {
        var session = session(login("admin@gmail.com", "secret"));

        var wrongHeader = importCsv(session, "fullName,email,role%nJane,jane@example.com,USER%n".formatted());
        assertThat(wrongHeader).bodyText().contains("Header must be: name,email,role");

        var duplicate = importCsv(
                session,
                "name,email,role%nAdmin Copy,admin@gmail.com,USER%nOther Copy,ADMIN@gmail.com,ADMIN%n".formatted());
        assertThat(duplicate)
                .bodyText()
                .contains("Email address is already in use", "Email is duplicated in the CSV file");
    }

    @Test
    void quotedFieldsAndWindowsLineEndingsAreSupported() {
        var email = "quoted+" + UUID.randomUUID() + "@acme.com";
        var csv = "\uFEFFname,email,role\r\n\"CSV, \"\"User\"\"\r\nSecond line\"," + email + ",USER";

        var result = userService.importUsers(csv);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
        assertThat(userService.findByEmail(email))
                .hasValueSatisfying(user -> assertThat(user.name()).isEqualTo("CSV, \"User\"\r\nSecond line"));
    }

    @Test
    void multilineFieldsPreserveErrorRowNumbers() {
        var result =
                userService.importUsers("name,email,role\r\n\"First\r\nSecond\",invalid,USER\r\nMissing Columns\r\n");

        assertThat(result.importedCount()).isZero();
        assertThat(result.errors()).extracting("rowNumber").containsExactly(2, 4);
        assertThat(result.errors())
                .extracting("message")
                .containsExactly("Email address must be valid", "Expected 3 columns");
    }

    @Test
    void unclosedQuotedFieldsAreRejected() {
        var result = userService.importUsers("name,email,role%n\"Unclosed".formatted());

        assertThat(result.importedCount()).isZero();
        assertThat(result.errors()).extracting("message").containsExactly("CSV contains an unclosed quoted field");
    }

    @Test
    void regularUserCannotImportUsers() {
        var session = session(login("siva@gmail.com", "secret"));
        assertThat(mvc.get().uri("/admin/users/import").session(session).exchange())
                .hasStatus(HttpStatus.FORBIDDEN);

        var result = importCsv(session, "name,email,role%nHacker,hacker@example.com,ADMIN%n".formatted());
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult importCsv(MockHttpSession session, String csv) {
        var file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        return mvc.perform(
                multipart("/admin/users/import").file(file).session(session).with(csrf()));
    }
}
