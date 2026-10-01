package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class CreateReplyTests extends BaseIT {
    private static final long ACTIVE_MESSAGE_ID = 3001L;
    private static final long DELETED_MESSAGE_ID = 3002L;

    @Test
    void activeUserCanReplyAsThemselvesAndReplyCountIsUpdated() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var replyContent = "Identified reply " + UUID.randomUUID();

        assertThat(createReply(userSession, ACTIVE_MESSAGE_ID, replyContent, "IDENTIFIED"))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + ACTIVE_MESSAGE_ID);

        var reply = findReply(ACTIVE_MESSAGE_ID, replyContent);
        assertThat(reply.get("created_by_user_id")).isEqualTo(2L);
        assertThat(reply.get("anonymous")).isEqualTo(false);
        assertThat(mvc.get()
                        .uri("/messages/{id}", ACTIVE_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Replies", ">1</dd>");
    }

    @Test
    void adminCanReplyAnonymouslyWithoutExposingCreatorToRegularUser() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var adminSession = session(login("admin@gmail.com", "secret"));
        var replyContent = "Private reply " + UUID.randomUUID();

        assertThat(createReply(adminSession, ACTIVE_MESSAGE_ID, replyContent, "ANONYMOUS"))
                .hasStatus(HttpStatus.FOUND);

        var reply = findReply(ACTIVE_MESSAGE_ID, replyContent);
        assertThat(reply.get("created_by_user_id")).isEqualTo(1L);
        assertThat(reply.get("anonymous")).isEqualTo(true);
        assertThat(mvc.get()
                        .uri("/messages/{id}", ACTIVE_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Anonymous")
                .doesNotContain("Admin", "admin@gmail.com");
    }

    @Test
    void replyContentAndPostingIdentityAreRequired() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.post()
                        .uri("/messages/{id}/replies", ACTIVE_MESSAGE_ID)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", "   ")
                        .session(userSession)
                        .with(csrf())
                        .exchange())
                .hasStatusOk()
                .hasViewName("messages/view")
                .bodyText()
                .contains("Reply is required", "Choose how to reply");
        assertThat(countReplies(ACTIVE_MESSAGE_ID)).isZero();
    }

    @Test
    void deletedMessageCannotBeRepliedTo() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(createReply(userSession, DELETED_MESSAGE_ID, "Not allowed", "IDENTIFIED"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(countReplies(DELETED_MESSAGE_ID)).isZero();
        assertThat(mvc.get()
                        .uri("/messages/{id}", DELETED_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .doesNotContain("Post reply");
    }

    @Test
    void unauthenticatedUserCannotCreateReply() {
        assertThat(mvc.post()
                        .uri("/messages/1/replies")
                        .param("content", "Not allowed")
                        .param("postingIdentity", "IDENTIFIED")
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private MvcTestResult createReply(MockHttpSession session, Long messageId, String content, String postingIdentity) {
        return mvc.post()
                .uri("/messages/{id}/replies", messageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .param("postingIdentity", postingIdentity)
                .session(session)
                .with(csrf())
                .exchange();
    }

    private Map<String, Object> findReply(Long messageId, String content) {
        return jdbcClient
                .sql("select created_by_user_id, anonymous from replies where message_id = ? and content = ?")
                .params(messageId, content)
                .query()
                .singleRow();
    }

    private long countReplies(Long messageId) {
        return jdbcClient
                .sql("select count(*) from replies where message_id = ?")
                .param(messageId)
                .query(Long.class)
                .single();
    }
}
