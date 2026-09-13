package dev.sivalabs.feedbackhub.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class EditMessageTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Test
    void ownerCanEditMessageContentAndUpdatedTimestamp() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var originalContent = "Original feedback " + UUID.randomUUID();
        createMessage(userSession, originalContent, "IDENTIFIED");
        var message = findMessage(originalContent);
        message.setUpdatedAt(Instant.EPOCH);
        messageRepository.saveAndFlush(message);
        var updatedContent = "Updated feedback " + UUID.randomUUID();

        assertThat(editMessage(userSession, message.getId(), updatedContent))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + message.getId());

        var updated = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(updated.getContent()).isEqualTo(updatedContent);
        assertThat(updated.getUpdatedAt()).isAfter(Instant.EPOCH);
        assertThat(updated.isAnonymous()).isFalse();
    }

    @Test
    void editingAnonymousMessagePreservesPostingIdentity() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var originalContent = "Anonymous original " + UUID.randomUUID();
        createMessage(userSession, originalContent, "ANONYMOUS");
        var message = findMessage(originalContent);

        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Edit message");
        assertThat(editMessage(userSession, message.getId(), "Anonymous updated " + UUID.randomUUID()))
                .hasStatus(HttpStatus.FOUND);

        assertThat(messageRepository.findById(message.getId()).orElseThrow().isAnonymous())
                .isTrue();
    }

    @Test
    void updatedContentMustNotBeEmpty() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = "Unchanged feedback " + UUID.randomUUID();
        createMessage(userSession, content, "IDENTIFIED");
        var message = findMessage(content);

        assertThat(editMessage(userSession, message.getId(), "   "))
                .hasStatusOk()
                .hasViewName("messages/edit")
                .bodyText()
                .contains("Message is required");
        assertThat(messageRepository.findById(message.getId()).orElseThrow().getContent())
                .isEqualTo(content);
    }

    @Test
    void anotherUserCannotEditMessage() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var content = "Other owner feedback " + UUID.randomUUID();
        createMessage(ownerSession, content, "ANONYMOUS");
        var message = findMessage(content);
        var otherUserSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}/edit", message.getId())
                        .session(otherUserSession)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(editMessage(otherUserSession, message.getId(), "Unauthorized change"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(messageRepository.findById(message.getId()).orElseThrow().getContent())
                .isEqualTo(content);
    }

    @Test
    void deletedMessageCannotBeEdited() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = "Deleted feedback " + UUID.randomUUID();
        createMessage(userSession, content, "IDENTIFIED");
        var message = findMessage(content);
        message.setStatus(MessageStatus.DELETED);
        messageRepository.saveAndFlush(message);

        assertThat(mvc.get()
                        .uri("/messages/{id}/edit", message.getId())
                        .session(userSession)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(editMessage(userSession, message.getId(), "Cannot update"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .doesNotContain("Edit message");
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

    private MessageEntity findMessage(String content) {
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(message -> message.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }

    private void createMessage(MockHttpSession session, String content, String postingIdentity) {
        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", postingIdentity)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }
}
