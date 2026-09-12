package dev.sivalabs.speakup.messages;

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

class CreateReplyTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ReplyRepository replyRepository;

    @Test
    void activeUserCanReplyAsThemselvesAndReplyCountIsUpdated() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession, "Message for reply " + UUID.randomUUID(), "IDENTIFIED");
        var replyContent = "Identified reply " + UUID.randomUUID();

        assertThat(createReply(userSession, message.getId(), replyContent, "IDENTIFIED"))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + message.getId());

        var reply = findReply(message.getId(), replyContent);
        assertThat(reply.getMessage().getId()).isEqualTo(message.getId());
        assertThat(reply.getCreator().getId()).isEqualTo(2L);
        assertThat(reply.isAnonymous()).isFalse();
        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Replies", "1");
    }

    @Test
    void adminCanReplyAnonymouslyWithoutExposingCreatorToRegularUser() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession, "Public question " + UUID.randomUUID(), "IDENTIFIED");
        var adminSession = session(login("admin@gmail.com", "secret"));
        var replyContent = "Private reply " + UUID.randomUUID();

        assertThat(createReply(adminSession, message.getId(), replyContent, "ANONYMOUS"))
                .hasStatus(HttpStatus.FOUND);

        var reply = findReply(message.getId(), replyContent);
        assertThat(reply.getCreator().getId()).isEqualTo(1L);
        assertThat(reply.isAnonymous()).isTrue();
        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Anonymous")
                .doesNotContain("Admin", "admin@gmail.com");
    }

    @Test
    void replyContentAndPostingIdentityAreRequired() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession, "Validation target " + UUID.randomUUID(), "IDENTIFIED");

        assertThat(mvc.post()
                        .uri("/messages/{id}/replies", message.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", "   ")
                        .session(userSession)
                        .with(csrf())
                        .exchange())
                .hasStatusOk()
                .hasViewName("messages/view")
                .bodyText()
                .contains("Reply is required", "Choose how to reply");
        assertThat(replyRepository.countByMessageIdAndStatus(message.getId(), ReplyStatus.ACTIVE))
                .isZero();
    }

    @Test
    void deletedMessageCannotBeRepliedTo() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession, "Deleted target " + UUID.randomUUID(), "IDENTIFIED");
        message.setStatus(MessageStatus.DELETED);
        messageRepository.saveAndFlush(message);

        assertThat(createReply(userSession, message.getId(), "Not allowed", "IDENTIFIED"))
                .hasStatusOk()
                .hasViewName("error/403");
        assertThat(replyRepository.countByMessageIdAndStatus(message.getId(), ReplyStatus.ACTIVE))
                .isZero();
        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
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

    private MessageEntity createMessage(MockHttpSession session, String content, String postingIdentity) {
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

    private ReplyEntity findReply(Long messageId, String content) {
        return replyRepository.findAllByMessageIdOrderByCreatedAtAsc(messageId).stream()
                .filter(reply -> reply.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }
}
