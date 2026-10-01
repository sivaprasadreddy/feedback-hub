package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class DeleteMessageTests extends BaseIT {
    private static final long OWNER_MESSAGE_ID = 2011L;
    private static final long ADMIN_MESSAGE_ID = 2012L;
    private static final long DELETED_MESSAGE_ID = 2013L;

    private static final String OWNER_CONTENT = "Delete seed owner content 2011";

    @Test
    void ownerCanSoftDeleteActiveMessageAndOriginalRecordIsRetained() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}", OWNER_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Delete message");
        assertThat(deleteMessage(userSession, OWNER_MESSAGE_ID))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + OWNER_MESSAGE_ID);

        assertThat(statusOf(OWNER_MESSAGE_ID)).isEqualTo("DELETED");
        assertThat(contentOf(OWNER_MESSAGE_ID)).isEqualTo(OWNER_CONTENT);
        assertThat(mvc.get()
                        .uri("/messages/{id}", OWNER_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(MessageService.DELETED_CONTENT)
                .doesNotContain(OWNER_CONTENT, "Delete message");
        assertThat(mvc.get().uri("/").session(userSession).exchange())
                .bodyText()
                .doesNotContain(OWNER_CONTENT);
    }

    @Test
    void anotherUserCannotDeleteMessage() {
        var otherUserSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteMessage(otherUserSession, ADMIN_MESSAGE_ID))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(statusOf(ADMIN_MESSAGE_ID)).isEqualTo("ACTIVE");
    }

    @Test
    void deletedMessageCannotBeDeletedAgain() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteMessage(userSession, DELETED_MESSAGE_ID))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
    }

    @Test
    void unauthenticatedUserCannotDeleteMessage() {
        assertThat(mvc.post().uri("/messages/1/delete").with(csrf()).exchange()).hasStatus(HttpStatus.FOUND);
    }

    private String statusOf(long messageId) {
        return jdbcClient
                .sql("select status from messages where id = ?")
                .param(messageId)
                .query(String.class)
                .single();
    }

    private String contentOf(long messageId) {
        return jdbcClient
                .sql("select content from messages where id = ?")
                .param(messageId)
                .query(String.class)
                .single();
    }

    private MvcTestResult deleteMessage(MockHttpSession session, Long messageId) {
        return mvc.post()
                .uri("/messages/{id}/delete", messageId)
                .session(session)
                .with(csrf())
                .exchange();
    }
}
