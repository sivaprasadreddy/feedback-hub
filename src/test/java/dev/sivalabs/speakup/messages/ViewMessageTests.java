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

class ViewMessageTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Test
    void activeUserCanViewMessageAndCurrentEngagementInformation() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = "Message details " + UUID.randomUUID();
        createMessage(userSession, content, "IDENTIFIED");
        var messageId = findMessage(content).getId();

        assertThat(mvc.get()
                        .uri("/messages/{id}", messageId)
                        .session(userSession)
                        .exchange())
                .hasStatusOk()
                .hasViewName("messages/view")
                .bodyText()
                .contains("Siva", content, "Upvotes", "Downvotes", "Replies")
                .doesNotContain("Your vote", "No vote");
    }

    @Test
    void anonymousMessageDoesNotExposeItsCreatorToRegularUser() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var content = "Private creator " + UUID.randomUUID();
        createMessage(adminSession, content, "ANONYMOUS");
        var messageId = findMessage(content).getId();
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}", messageId)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Anonymous", content)
                .doesNotContain("Admin", "admin@gmail.com");
    }

    @Test
    void deletedMessageShowsPlaceholderInsteadOfOriginalContent() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = "Deleted content " + UUID.randomUUID();
        createMessage(userSession, content, "IDENTIFIED");
        var message = findMessage(content);
        message.setStatus(MessageStatus.DELETED);
        messageRepository.saveAndFlush(message);

        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(MessageService.DELETED_CONTENT)
                .doesNotContain(content);

        assertThat(mvc.get().uri("/").session(userSession).exchange())
                .bodyText()
                .contains(MessageService.DELETED_CONTENT)
                .doesNotContain(content);
    }

    @Test
    void unauthenticatedAndInactiveUsersCannotViewMessageDetails() {
        assertThat(mvc.get().uri("/messages/1").exchange()).hasStatus(HttpStatus.FOUND);
        assertThat(login("prasad@gmail.com", "secret"))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login?error");
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
