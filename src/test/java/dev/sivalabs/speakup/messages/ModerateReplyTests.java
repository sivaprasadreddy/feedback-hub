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
class ModerateReplyTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ReplyRepository replyRepository;

    @Test
    void adminCanDeleteAnotherUsersReplyWithAuditAndAssociationsPreserved() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession);
        var content = "Admin-deleted reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), content, "IDENTIFIED");
        var reply = findReply(message.getId(), content);
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(deleteAsAdmin(adminSession, reply.getId()))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/admin/replies");

        var deleted = replyRepository.findById(reply.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(ReplyStatus.DELETED);
        assertThat(deleted.getContent()).isEqualTo(content);
        assertThat(deleted.getMessage().getId()).isEqualTo(message.getId());
        assertThat(deleted.getDeletedByAdminUserId()).isEqualTo(1L);
        assertThat(deleted.getDeletedByAdminAt()).isNotNull();
        assertThat(replyRepository.countByMessageIdAndStatus(message.getId(), ReplyStatus.ACTIVE))
                .isZero();
    }

    @Test
    void adminDeletedAnonymousReplyShowsPlaceholderWithoutExposingCreator() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession);
        var content = "Anonymous admin-deleted reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), content, "ANONYMOUS");
        var reply = findReply(message.getId(), content);
        var adminSession = session(login("admin@gmail.com", "secret"));
        deleteAsAdmin(adminSession, reply.getId());

        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Anonymous", MessageService.DELETED_REPLY_CONTENT, ">0</dd>")
                .doesNotContain(content, "siva@gmail.com");
    }

    @Test
    void regularAndUnauthenticatedUsersCannotAdministrativelyDeleteReply() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(adminSession);
        var content = "Protected reply " + UUID.randomUUID();
        createReply(adminSession, message.getId(), content, "IDENTIFIED");
        var reply = findReply(message.getId(), content);
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteAsAdmin(userSession, reply.getId())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post()
                        .uri("/admin/replies/{id}/delete", reply.getId())
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        assertThat(replyRepository.findById(reply.getId()).orElseThrow().getStatus())
                .isEqualTo(ReplyStatus.ACTIVE);
    }

    @Test
    void adminCanReviewRepliesAndCannotDeleteDeletedReplyAgain() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession);
        var content = "Reply review target " + UUID.randomUUID();
        createReply(userSession, message.getId(), content, "IDENTIFIED");
        var reply = findReply(message.getId(), content);
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/replies").session(adminSession).exchange())
                .hasStatusOk()
                .hasViewName("admin/replies")
                .bodyText()
                .contains(content, "Delete reply", "View discussion");
        deleteAsAdmin(adminSession, reply.getId());
        assertThat(deleteAsAdmin(adminSession, reply.getId())).hasStatusOk().hasViewName("error/403");
    }

    private MessageEntity createMessage(MockHttpSession session) {
        var content = "Message for admin-deleted reply " + UUID.randomUUID();
        mvc.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .param("postingIdentity", "IDENTIFIED")
                .session(session)
                .with(csrf())
                .exchange();
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(message -> message.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }

    private void createReply(MockHttpSession session, Long messageId, String content, String identity) {
        mvc.post()
                .uri("/messages/{id}/replies", messageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .param("postingIdentity", identity)
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

    private MvcTestResult deleteAsAdmin(MockHttpSession session, Long replyId) {
        return mvc.post()
                .uri("/admin/replies/{id}/delete", replyId)
                .session(session)
                .with(csrf())
                .exchange();
    }
}
