package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class EditMessageTests extends BaseIT {
    private static final long OWNER_MESSAGE_ID = 2001L;
    private static final long ANONYMOUS_MESSAGE_ID = 2002L;
    private static final long UNCHANGED_MESSAGE_ID = 2003L;
    private static final long ADMIN_MESSAGE_ID = 2004L;
    private static final long DELETED_MESSAGE_ID = 2005L;

    private static final String UNCHANGED_CONTENT = "Edit seed unchanged content 2003";
    private static final String ADMIN_MESSAGE_CONTENT = "Edit seed other owner content 2004";

    @Test
    void ownerCanEditMessageContentAndUpdatedTimestamp() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var updatedContent = "Updated feedback " + UUID.randomUUID();

        assertThat(editMessage(userSession, OWNER_MESSAGE_ID, updatedContent))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + OWNER_MESSAGE_ID);

        assertThat(contentOf(OWNER_MESSAGE_ID)).isEqualTo(updatedContent);
        assertThat(updatedAtOf(OWNER_MESSAGE_ID)).isAfter(LocalDateTime.of(1970, 1, 1, 0, 0));
        assertThat(isAnonymous(OWNER_MESSAGE_ID)).isFalse();
    }

    @Test
    void editingAnonymousMessagePreservesPostingIdentity() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}", ANONYMOUS_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Edit message");
        assertThat(editMessage(userSession, ANONYMOUS_MESSAGE_ID, "Anonymous updated " + UUID.randomUUID()))
                .hasStatus(HttpStatus.FOUND);

        assertThat(isAnonymous(ANONYMOUS_MESSAGE_ID)).isTrue();
    }

    @Test
    void updatedContentMustNotBeEmpty() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(editMessage(userSession, UNCHANGED_MESSAGE_ID, "   "))
                .hasStatusOk()
                .hasViewName("messages/edit")
                .bodyText()
                .contains("Message is required");
        assertThat(contentOf(UNCHANGED_MESSAGE_ID)).isEqualTo(UNCHANGED_CONTENT);
    }

    @Test
    void anotherUserCannotEditMessage() {
        var otherUserSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}/edit", ADMIN_MESSAGE_ID)
                        .session(otherUserSession)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(editMessage(otherUserSession, ADMIN_MESSAGE_ID, "Unauthorized change"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(contentOf(ADMIN_MESSAGE_ID)).isEqualTo(ADMIN_MESSAGE_CONTENT);
    }

    @Test
    void deletedMessageCannotBeEdited() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}/edit", DELETED_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(editMessage(userSession, DELETED_MESSAGE_ID, "Cannot update"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(mvc.get()
                        .uri("/messages/{id}", DELETED_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .doesNotContain("Edit message");
    }

    private String contentOf(long messageId) {
        return jdbcClient
                .sql("select content from messages where id = ?")
                .param(messageId)
                .query(String.class)
                .single();
    }

    private LocalDateTime updatedAtOf(long messageId) {
        return jdbcClient
                .sql("select updated_at from messages where id = ?")
                .param(messageId)
                .query(LocalDateTime.class)
                .single();
    }

    private boolean isAnonymous(long messageId) {
        return jdbcClient
                .sql("select anonymous from messages where id = ?")
                .param(messageId)
                .query(Boolean.class)
                .single();
    }

    private MvcTestResult editMessage(MockHttpSession session, Long messageId, String content) {
        return mvc.post()
                .uri("/messages/{id}/edit", messageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .session(session)
                .with(csrf())
                .exchange();
    }
}
