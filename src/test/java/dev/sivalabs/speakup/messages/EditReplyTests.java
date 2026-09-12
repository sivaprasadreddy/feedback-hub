package dev.sivalabs.speakup.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.speakup.BaseIT;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class EditReplyTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ReplyRepository replyRepository;

    @Test
    void ownerCanEditReplyContentAndUpdatedTimestamp() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession);
        var originalContent = "Original reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), originalContent, "IDENTIFIED");
        var reply = findReply(message.getId(), originalContent);
        reply.setUpdatedAt(Instant.EPOCH);
        replyRepository.saveAndFlush(reply);
        var updatedContent = "Updated reply " + UUID.randomUUID();

        assertThat(editReply(userSession, message.getId(), reply.getId(), updatedContent))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + message.getId());

        var updated = replyRepository.findById(reply.getId()).orElseThrow();
        assertThat(updated.getContent()).isEqualTo(updatedContent);
        assertThat(updated.getUpdatedAt()).isAfter(Instant.EPOCH);
        assertThat(updated.isAnonymous()).isFalse();
    }

    @Test
    void editingAnonymousReplyPreservesPostingIdentity() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession);
        var originalContent = "Anonymous reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), originalContent, "ANONYMOUS");
        var reply = findReply(message.getId(), originalContent);

        assertThat(mvc.get()
                        .uri("/messages/{messageId}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Edit reply");
        assertThat(editReply(userSession, message.getId(), reply.getId(), "Anonymous update " + UUID.randomUUID()))
                .hasStatus(HttpStatus.FOUND);

        assertThat(replyRepository.findById(reply.getId()).orElseThrow().isAnonymous())
                .isTrue();
    }

    @Test
    void updatedContentMustNotBeEmpty() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession);
        var content = "Unchanged reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), content, "IDENTIFIED");
        var reply = findReply(message.getId(), content);

        assertThat(editReply(userSession, message.getId(), reply.getId(), "   "))
                .hasStatusOk()
                .hasViewName("messages/edit-reply")
                .bodyText()
                .contains("Reply is required");
        assertThat(replyRepository.findById(reply.getId()).orElseThrow().getContent())
                .isEqualTo(content);
    }

    @Test
    void anotherUserCannotEditReply() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession, "ANONYMOUS");
        var content = "Other owner's reply " + UUID.randomUUID();
        createReply(ownerSession, message.getId(), content, "IDENTIFIED");
        var reply = findReply(message.getId(), content);
        var otherUserSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{messageId}/replies/{replyId}/edit", message.getId(), reply.getId())
                        .session(otherUserSession)
                        .exchange())
                .hasStatusOk()
                .hasViewName("error/403");
        assertThat(editReply(otherUserSession, message.getId(), reply.getId(), "Unauthorized change"))
                .hasStatusOk()
                .hasViewName("error/403");
        assertThat(replyRepository.findById(reply.getId()).orElseThrow().getContent())
                .isEqualTo(content);
    }

    @Test
    void deletedReplyCannotBeEditedAndHasNoEditLink() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession);
        var content = "Deleted reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), content, "IDENTIFIED");
        var reply = findReply(message.getId(), content);
        reply.setStatus(ReplyStatus.DELETED);
        replyRepository.saveAndFlush(reply);

        assertThat(mvc.get()
                        .uri("/messages/{messageId}/replies/{replyId}/edit", message.getId(), reply.getId())
                        .session(userSession)
                        .exchange())
                .hasStatusOk()
                .hasViewName("error/403");
        assertThat(editReply(userSession, message.getId(), reply.getId(), "Cannot update"))
                .hasStatusOk()
                .hasViewName("error/403");
        assertThat(mvc.get()
                        .uri("/messages/{messageId}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .doesNotContain("Edit reply");
    }

    private MessageEntity createMessage(MockHttpSession session) {
        return createMessage(session, "IDENTIFIED");
    }

    private MessageEntity createMessage(MockHttpSession session, String postingIdentity) {
        var content = "Message for edited reply " + UUID.randomUUID();
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

    private MvcTestResult editReply(MockHttpSession session, Long messageId, Long replyId, String content) {
        return mvc.post()
                .uri("/messages/{messageId}/replies/{replyId}/edit", messageId, replyId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
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

    private MvcTestResult login(String email, String password) {
        return mvc.post()
                .uri("/login")
                .param("username", email)
                .param("password", password)
                .with(csrf())
                .exchange();
    }

    private static MockHttpSession session(MvcTestResult result) {
        return (MockHttpSession) result.getMvcResult().getRequest().getSession(false);
    }
}
