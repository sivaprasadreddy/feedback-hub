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

class DeleteMessageTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Test
    void ownerCanSoftDeleteActiveMessageAndOriginalRecordIsRetained() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = "Feedback to delete " + UUID.randomUUID();
        createMessage(userSession, content, "IDENTIFIED");
        var message = findMessage(content);

        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Delete message");
        assertThat(deleteMessage(userSession, message.getId()))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + message.getId());

        var deleted = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(MessageStatus.DELETED);
        assertThat(deleted.getContent()).isEqualTo(content);
        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(MessageService.DELETED_CONTENT)
                .doesNotContain(content, "Delete message");
        assertThat(mvc.get().uri("/").session(userSession).exchange())
                .bodyText()
                .contains(MessageService.DELETED_CONTENT)
                .doesNotContain(content);
    }

    @Test
    void anotherUserCannotDeleteMessage() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var content = "Protected anonymous feedback " + UUID.randomUUID();
        createMessage(ownerSession, content, "ANONYMOUS");
        var message = findMessage(content);
        var otherUserSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteMessage(otherUserSession, message.getId()))
                .hasStatusOk()
                .hasViewName("error/403");
        assertThat(messageRepository.findById(message.getId()).orElseThrow().getStatus())
                .isEqualTo(MessageStatus.ACTIVE);
    }

    @Test
    void deletedMessageCannotBeDeletedAgain() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = "Already deleted feedback " + UUID.randomUUID();
        createMessage(userSession, content, "IDENTIFIED");
        var message = findMessage(content);
        message.setStatus(MessageStatus.DELETED);
        messageRepository.saveAndFlush(message);

        assertThat(deleteMessage(userSession, message.getId())).hasStatusOk().hasViewName("error/403");
    }

    @Test
    void unauthenticatedUserCannotDeleteMessage() {
        assertThat(mvc.post().uri("/messages/1/delete").with(csrf()).exchange()).hasStatus(HttpStatus.FOUND);
    }

    private MvcTestResult deleteMessage(MockHttpSession session, Long messageId) {
        return mvc.post()
                .uri("/messages/{id}/delete", messageId)
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
