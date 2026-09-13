package dev.sivalabs.feedbackhub.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ModerateMessageTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ReplyRepository replyRepository;

    @MockitoBean
    MessageAnalyzer messageAnalyzer;

    @Test
    void adminCanDeleteAnotherUsersMessageWithAuditAndAssociationsPreserved() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = createMessage(userSession, "Admin-deleted message " + UUID.randomUUID(), "IDENTIFIED");
        var message = findMessage(content);
        createReply(userSession, message.getId(), "Preserved reply " + UUID.randomUUID());
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/delete", message.getId())
                        .session(adminSession)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/admin/messages");

        var deleted = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(MessageStatus.DELETED);
        assertThat(deleted.getDeletedByAdminUserId()).isEqualTo(1L);
        assertThat(deleted.getDeletedByAdminAt()).isNotNull();
        assertThat(replyRepository.countByMessageIdAndStatus(message.getId(), ReplyStatus.ACTIVE))
                .isOne();
    }

    @Test
    void adminDeletedAnonymousMessageShowsPlaceholderWithoutExposingCreator() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = createMessage(userSession, "Anonymous admin deletion " + UUID.randomUUID(), "ANONYMOUS");
        var message = findMessage(content);
        var adminSession = session(login("admin@gmail.com", "secret"));
        deleteAsAdmin(adminSession, message.getId());

        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Anonymous", MessageService.DELETED_CONTENT)
                .doesNotContain(content, "siva@gmail.com");
    }

    @Test
    void regularAndUnauthenticatedUsersCannotAdministrativelyDelete() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var content = createMessage(ownerSession, "Protected from user deletion " + UUID.randomUUID(), "IDENTIFIED");
        var message = findMessage(content);
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteAsAdmin(userSession, message.getId())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/delete", message.getId())
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        assertThat(messageRepository.findById(message.getId()).orElseThrow().getStatus())
                .isEqualTo(MessageStatus.ACTIVE);
    }

    @Test
    void adminCanReviewMessagesAndCannotDeleteDeletedMessageAgain() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = createMessage(userSession, "Review target " + UUID.randomUUID(), "IDENTIFIED");
        var message = findMessage(content);
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/messages").session(adminSession).exchange())
                .hasStatusOk()
                .hasViewName("admin/messages")
                .bodyText()
                .contains(content, "Delete message");
        deleteAsAdmin(adminSession, message.getId());
        assertThat(deleteAsAdmin(adminSession, message.getId()))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
    }

    @Test
    void adminCanAnalyzeUnanalyzedMessageWithHtmx() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = createMessage(userSession, "Analyze from admin " + UUID.randomUUID(), "IDENTIFIED");
        var message = findMessage(content);
        var adminSession = session(login("admin@gmail.com", "secret"));
        when(messageAnalyzer.analyze(content))
                .thenReturn(new MessageAnalysis(
                        Set.of(MessageTopic.WORK_CULTURE, MessageTopic.PEOPLE_AND_TEAM), MessageSentiment.HAPPY));

        assertThat(mvc.get().uri("/admin/messages").session(adminSession).exchange())
                .bodyText()
                .contains(
                        content,
                        "Analyze message",
                        "hx-post=\"/admin/messages/" + message.getId() + "/analyze\"",
                        "hx-target=\"closest .admin-message\"",
                        "hx-swap=\"outerHTML\"");

        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/analyze", message.getId())
                        .header("HX-Request", "true")
                        .session(adminSession)
                        .with(csrf())
                        .exchange())
                .hasStatusOk()
                .hasViewName("fragments/admin-message :: message(message=${message})")
                .bodyText()
                .contains(content, "Happy", "Work Culture", "People &amp; Team")
                .doesNotContain("Analyze message", "<html");

        var analyzed = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(analyzed.getSentiment()).isEqualTo(MessageSentiment.HAPPY);
        assertThat(analyzed.getTopics()).containsExactlyInAnyOrder("Work Culture", "People & Team");
    }

    @Test
    void analysisErrorKeepsPreviousMessageDetailsAndShowsRetryOption() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = createMessage(userSession, "Failed admin analysis " + UUID.randomUUID(), "IDENTIFIED");
        var message = findMessage(content);
        var adminSession = session(login("admin@gmail.com", "secret"));
        when(messageAnalyzer.analyze(content)).thenThrow(new IllegalStateException("AI unavailable"));

        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/analyze", message.getId())
                        .header("HX-Request", "true")
                        .session(adminSession)
                        .with(csrf())
                        .exchange())
                .hasStatusOk()
                .bodyText()
                .contains(
                        content,
                        "Something went wrong while analyzing this message. Please try again.",
                        "Analyze message",
                        "Delete message")
                .doesNotContain("Internal Server Error", "<html");

        var unchanged = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(unchanged.getSentiment()).isNull();
        assertThat(unchanged.getTopics()).isEmpty();
    }

    @Test
    void adminMessageListIsPaginated() {
        for (int index = 0; index < MessageService.ADMIN_PAGE_SIZE + 1; index++) {
            var message = new MessageEntity();
            message.setContent("Paginated admin message " + index);
            message.setCreatorUserId(2L);
            messageRepository.save(message);
        }
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/messages").session(adminSession).exchange())
                .hasStatusOk()
                .bodyText()
                .contains("Page 1 of", "page=2");
        assertThat(mvc.get().uri("/admin/messages?page=2").session(adminSession).exchange())
                .hasStatusOk()
                .bodyText()
                .contains("Page 2 of", "page=1");
        assertThat(mvc.get().uri("/admin/messages?page=0").session(adminSession).exchange())
                .hasStatus(HttpStatus.BAD_REQUEST);
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

    private MvcTestResult deleteAsAdmin(MockHttpSession session, Long messageId) {
        return mvc.post()
                .uri("/admin/messages/{id}/delete", messageId)
                .session(session)
                .with(csrf())
                .exchange();
    }
}
