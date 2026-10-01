package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class DeleteReplyTests extends BaseIT {
    private static final long OWN_MESSAGE_ID = 3010L;
    private static final long OWN_REPLY_ID = 3010L;
    private static final String OWN_REPLY_CONTENT = "Reply to delete 3010";
    private static final long ADMIN_MESSAGE_ID = 3011L;
    private static final long ADMIN_REPLY_ID = 3011L;
    private static final long DELETED_MESSAGE_ID = 3012L;
    private static final long DELETED_REPLY_ID = 3012L;

    @Test
    void ownerCanSoftDeleteActiveReplyAndReplyCountIsUpdated() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{messageId}", OWN_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Delete reply", ">1</dd>");
        assertThat(deleteReply(userSession, OWN_MESSAGE_ID, OWN_REPLY_ID))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + OWN_MESSAGE_ID);

        var row = jdbcClient
                .sql("select status, content from replies where id = ?")
                .param(OWN_REPLY_ID)
                .query()
                .singleRow();
        assertThat(row.get("status")).isEqualTo("DELETED");
        assertThat(row.get("content")).isEqualTo(OWN_REPLY_CONTENT);
        assertThat(mvc.get()
                        .uri("/messages/{messageId}", OWN_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(MessageService.DELETED_REPLY_CONTENT, ">0</dd>")
                .doesNotContain(OWN_REPLY_CONTENT, "Delete reply");
    }

    @Test
    void anotherUserCannotDeleteReply() {
        var otherUserSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteReply(otherUserSession, ADMIN_MESSAGE_ID, ADMIN_REPLY_ID))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        var status = jdbcClient
                .sql("select status from replies where id = ?")
                .param(ADMIN_REPLY_ID)
                .query(String.class)
                .single();
        assertThat(status).isEqualTo("ACTIVE");
    }

    @Test
    void deletedReplyCannotBeDeletedAgain() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteReply(userSession, DELETED_MESSAGE_ID, DELETED_REPLY_ID))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
    }

    @Test
    void unauthenticatedUserCannotDeleteReply() {
        assertThat(mvc.post().uri("/messages/1/replies/1/delete").with(csrf()).exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private MvcTestResult deleteReply(MockHttpSession session, Long messageId, Long replyId) {
        return mvc.post()
                .uri("/messages/{messageId}/replies/{replyId}/delete", messageId, replyId)
                .session(session)
                .with(csrf())
                .exchange();
    }
}
