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
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ModerateMessageTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ReplyRepository replyRepository;

    @Test
    void adminCanModerateAnotherUsersMessageWithAuditAndAssociationsPreserved() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = createMessage(userSession, "Moderated message " + UUID.randomUUID(), "IDENTIFIED");
        var message = findMessage(content);
        createReply(userSession, message.getId(), "Preserved reply " + UUID.randomUUID());
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/moderate", message.getId())
                        .session(adminSession)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/admin/messages");

        var moderated = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(moderated.getStatus()).isEqualTo(MessageStatus.DELETED);
        assertThat(moderated.getModerator().getId()).isEqualTo(1L);
        assertThat(moderated.getModeratedAt()).isNotNull();
        assertThat(replyRepository.countByMessageIdAndStatus(message.getId(), ReplyStatus.ACTIVE))
                .isOne();
    }

    @Test
    void moderatedAnonymousMessageShowsPlaceholderWithoutExposingCreator() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = createMessage(userSession, "Anonymous moderated " + UUID.randomUUID(), "ANONYMOUS");
        var message = findMessage(content);
        var adminSession = session(login("admin@gmail.com", "secret"));
        moderate(adminSession, message.getId());

        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Anonymous", MessageService.DELETED_CONTENT)
                .doesNotContain(content, "siva@gmail.com");
    }

    @Test
    void regularAndUnauthenticatedUsersCannotAdministrativelyModerate() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var content = createMessage(ownerSession, "Protected from user moderation " + UUID.randomUUID(), "IDENTIFIED");
        var message = findMessage(content);
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(moderate(userSession, message.getId())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/moderate", message.getId())
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        assertThat(messageRepository.findById(message.getId()).orElseThrow().getStatus())
                .isEqualTo(MessageStatus.ACTIVE);
    }

    @Test
    void adminCanReviewMessagesAndCannotModerateDeletedMessageAgain() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = createMessage(userSession, "Review target " + UUID.randomUUID(), "IDENTIFIED");
        var message = findMessage(content);
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/messages").session(adminSession).exchange())
                .hasStatusOk()
                .hasViewName("admin/messages")
                .bodyText()
                .contains(content, "Moderate message");
        moderate(adminSession, message.getId());
        assertThat(moderate(adminSession, message.getId())).hasStatusOk().hasViewName("error/403");
    }

    private String createMessage(MockHttpSession session, String content, String identity) {
        mvc.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .param("postingIdentity", identity)
                .session(session)
                .with(csrf())
                .exchange();
        return content;
    }

    private void createReply(MockHttpSession session, Long messageId, String content) {
        mvc.post()
                .uri("/messages/{id}/replies", messageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .param("postingIdentity", "IDENTIFIED")
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

    private MvcTestResult moderate(MockHttpSession session, Long messageId) {
        return mvc.post()
                .uri("/admin/messages/{id}/moderate", messageId)
                .session(session)
                .with(csrf())
                .exchange();
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
