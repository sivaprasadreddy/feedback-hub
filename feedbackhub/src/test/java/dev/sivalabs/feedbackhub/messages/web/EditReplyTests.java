package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class EditReplyTests extends BaseIT {
    private static final long MESSAGE_ID = 3020L;
    private static final long IDENTIFIED_REPLY_ID = 3020L;
    private static final long ANONYMOUS_REPLY_ID = 3021L;
    private static final long ADMIN_REPLY_ID = 3022L;
    private static final long DELETED_REPLY_ID = 3023L;

    @Test
    void ownerCanEditReplyContentAndUpdatedTimestamp() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var updatedContent = "Updated reply " + UUID.randomUUID();

        assertThat(editReply(userSession, MESSAGE_ID, IDENTIFIED_REPLY_ID, updatedContent))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + MESSAGE_ID);

        assertThat(replyContent(IDENTIFIED_REPLY_ID)).isEqualTo(updatedContent);
        var updatedAt = jdbcClient
                .sql("select updated_at from replies where id = ?")
                .param(IDENTIFIED_REPLY_ID)
                .query(Timestamp.class)
                .single();
        assertThat(updatedAt.toLocalDateTime()).isAfter(LocalDateTime.of(2001, 1, 1, 0, 0));
        assertThat(isAnonymous(IDENTIFIED_REPLY_ID)).isFalse();
    }

    @Test
    void editingAnonymousReplyPreservesPostingIdentity() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{messageId}", MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Edit reply");
        assertThat(editReply(userSession, MESSAGE_ID, ANONYMOUS_REPLY_ID, "Anonymous update " + UUID.randomUUID()))
                .hasStatus(HttpStatus.FOUND);

        assertThat(isAnonymous(ANONYMOUS_REPLY_ID)).isTrue();
    }

    @Test
    void updatedContentMustNotBeEmpty() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var original = replyContent(IDENTIFIED_REPLY_ID);

        assertThat(editReply(userSession, MESSAGE_ID, IDENTIFIED_REPLY_ID, "   "))
                .hasStatusOk()
                .hasViewName("messages/edit-reply")
                .bodyText()
                .contains("Reply is required");
        assertThat(replyContent(IDENTIFIED_REPLY_ID)).isEqualTo(original);
    }

    @Test
    void anotherUserCannotEditReply() {
        var otherUserSession = session(login("siva@gmail.com", "secret"));
        var original = replyContent(ADMIN_REPLY_ID);

        assertThat(mvc.get()
                        .uri("/messages/{messageId}/replies/{replyId}/edit", MESSAGE_ID, ADMIN_REPLY_ID)
                        .session(otherUserSession)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(editReply(otherUserSession, MESSAGE_ID, ADMIN_REPLY_ID, "Unauthorized change"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(replyContent(ADMIN_REPLY_ID)).isEqualTo(original);
    }

    @Test
    void deletedReplyCannotBeEditedAndHasNoEditLink() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var editedReplyUrl = "/messages/{messageId}/replies/{replyId}/edit";

        assertThat(mvc.get()
                        .uri(editedReplyUrl, MESSAGE_ID, DELETED_REPLY_ID)
                        .session(userSession)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(editReply(userSession, MESSAGE_ID, DELETED_REPLY_ID, "Cannot update"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(replyContent(DELETED_REPLY_ID)).isNotEqualTo("Cannot update");
    }

    private MvcTestResult editReply(MockHttpSession session, Long messageId, Long replyId, String content) {
        return mvc.post()
                .uri("/messages/{messageId}/replies/{replyId}/edit", messageId, replyId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .session(session)
                .with(csrf())
                .exchange();
    }

    private String replyContent(Long replyId) {
        return jdbcClient
                .sql("select content from replies where id = ?")
                .param(replyId)
                .query(String.class)
                .single();
    }

    private boolean isAnonymous(Long replyId) {
        return jdbcClient
                .sql("select anonymous from replies where id = ?")
                .param(replyId)
                .query(Boolean.class)
                .single();
    }
}
