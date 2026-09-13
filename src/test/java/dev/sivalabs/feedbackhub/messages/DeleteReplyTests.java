package dev.sivalabs.feedbackhub.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class DeleteReplyTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ReplyRepository replyRepository;

    @Test
    void ownerCanSoftDeleteActiveReplyAndReplyCountIsUpdated() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession, "IDENTIFIED");
        var content = "Reply to delete " + UUID.randomUUID();
        createReply(userSession, message.getId(), content, "IDENTIFIED");
        var reply = findReply(message.getId(), content);

        assertThat(mvc.get()
                        .uri("/messages/{messageId}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Delete reply", ">1</dd>");
        assertThat(deleteReply(userSession, message.getId(), reply.getId()))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + message.getId());

        var deleted = replyRepository.findById(reply.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(ReplyStatus.DELETED);
        assertThat(deleted.getContent()).isEqualTo(content);
        assertThat(replyRepository.countByMessageIdAndStatus(message.getId(), ReplyStatus.ACTIVE))
                .isZero();
        assertThat(mvc.get()
                        .uri("/messages/{messageId}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(MessageService.DELETED_REPLY_CONTENT, ">0</dd>")
                .doesNotContain(content, "Delete reply");
    }

    @Test
    void anotherUserCannotDeleteReply() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession, "ANONYMOUS");
        var content = "Protected anonymous reply " + UUID.randomUUID();
        createReply(ownerSession, message.getId(), content, "ANONYMOUS");
        var reply = findReply(message.getId(), content);
        var otherUserSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteReply(otherUserSession, message.getId(), reply.getId()))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(replyRepository.findById(reply.getId()).orElseThrow().getStatus())
                .isEqualTo(ReplyStatus.ACTIVE);
    }

    @Test
    void deletedReplyCannotBeDeletedAgain() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession, "IDENTIFIED");
        var content = "Already deleted reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), content, "IDENTIFIED");
        var reply = findReply(message.getId(), content);
        reply.setStatus(ReplyStatus.DELETED);
        replyRepository.saveAndFlush(reply);

        assertThat(deleteReply(userSession, message.getId(), reply.getId()))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
    }

    @Test
    void unauthenticatedUserCannotDeleteReply() {
        assertThat(mvc.post().uri("/messages/1/replies/1/delete").with(csrf()).exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private MessageEntity createMessage(MockHttpSession session, String postingIdentity) {
        var content = "Message for deleted reply " + UUID.randomUUID();
        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", postingIdentity)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(message -> message.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }

    private void createReply(MockHttpSession session, Long messageId, String content, String postingIdentity) {
        assertThat(mvc.post()
                        .uri("/messages/{messageId}/replies", messageId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", postingIdentity)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private MvcTestResult deleteReply(MockHttpSession session, Long messageId, Long replyId) {
        return mvc.post()
                .uri("/messages/{messageId}/replies/{replyId}/delete", messageId, replyId)
                .session(session)
                .with(csrf())
                .exchange();
    }

    private ReplyEntity findReply(Long messageId, String content) {
        return replyRepository.findAllByMessageIdOrderByCreatedAtAsc(messageId).stream()
                .filter(reply -> reply.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }
}
