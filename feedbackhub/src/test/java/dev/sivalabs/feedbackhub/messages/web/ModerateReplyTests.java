package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ModerateReplyTests extends BaseIT {
    private static final long DELETE_MESSAGE_ID = 5007L;
    private static final long DELETE_REPLY_ID = 5101L;
    private static final String DELETE_REPLY_CONTENT = "Moderation admin-deleted reply";
    private static final long ANONYMOUS_MESSAGE_ID = 5008L;
    private static final long ANONYMOUS_REPLY_ID = 5102L;
    private static final String ANONYMOUS_REPLY_CONTENT = "Moderation anonymous admin-deleted reply";
    private static final long PROTECTED_REPLY_ID = 5103L;
    private static final long REVIEW_REPLY_ID = 5104L;
    private static final String REVIEW_REPLY_CONTENT = "Moderation reply review target";
    private static final String SPAM_REPLY_CONTENT = "Moderation spam reply for admin";

    @Test
    void adminCanDeleteAnotherUsersReplyWithAuditAndAssociationsPreserved() {
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(deleteAsAdmin(adminSession, DELETE_REPLY_ID))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/admin/replies");

        entityManager.flush();
        assertThat(status(DELETE_REPLY_ID)).isEqualTo("DELETED");
        assertThat(jdbcClient
                        .sql("select content from replies where id = :id")
                        .param("id", DELETE_REPLY_ID)
                        .query(String.class)
                        .single())
                .isEqualTo(DELETE_REPLY_CONTENT);
        assertThat(jdbcClient
                        .sql("select message_id from replies where id = :id")
                        .param("id", DELETE_REPLY_ID)
                        .query(Long.class)
                        .single())
                .isEqualTo(DELETE_MESSAGE_ID);
        assertThat(jdbcClient
                        .sql("select deleted_by_admin_user_id from replies where id = :id")
                        .param("id", DELETE_REPLY_ID)
                        .query(Long.class)
                        .single())
                .isEqualTo(1L);
        assertThat(jdbcClient
                        .sql("select deleted_by_admin_at is not null from replies where id = :id")
                        .param("id", DELETE_REPLY_ID)
                        .query(Boolean.class)
                        .single())
                .isTrue();
        assertThat(jdbcClient
                        .sql("select count(*) from replies where message_id = :id and status = 'ACTIVE'")
                        .param("id", DELETE_MESSAGE_ID)
                        .query(Long.class)
                        .single())
                .isEqualTo(2L);
    }

    @Test
    void adminDeletedAnonymousReplyShowsPlaceholderWithoutExposingCreator() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var adminSession = session(login("admin@gmail.com", "secret"));
        deleteAsAdmin(adminSession, ANONYMOUS_REPLY_ID);

        assertThat(mvc.get()
                        .uri("/messages/{id}", ANONYMOUS_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Anonymous", MessageService.DELETED_REPLY_CONTENT, ">0</dd>")
                .doesNotContain(ANONYMOUS_REPLY_CONTENT, "siva@gmail.com");
    }

    @Test
    void regularAndUnauthenticatedUsersCannotAdministrativelyDeleteReply() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteAsAdmin(userSession, PROTECTED_REPLY_ID)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post()
                        .uri("/admin/replies/{id}/delete", PROTECTED_REPLY_ID)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        entityManager.flush();
        assertThat(status(PROTECTED_REPLY_ID)).isEqualTo("ACTIVE");
    }

    @Test
    void adminCanReviewRepliesAndCannotDeleteDeletedReplyAgain() {
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/replies").session(adminSession).exchange())
                .hasStatusOk()
                .hasViewName("admin/replies")
                .bodyText()
                .contains(REVIEW_REPLY_CONTENT, "Delete reply", "View discussion");
        deleteAsAdmin(adminSession, REVIEW_REPLY_ID);
        assertThat(deleteAsAdmin(adminSession, REVIEW_REPLY_ID))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
    }

    @Test
    void adminCanSeeWhenReplyIsSpam() {
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/replies").session(adminSession).exchange())
                .hasStatusOk()
                .bodyText()
                .contains(SPAM_REPLY_CONTENT, "Spam");
    }

    @Test
    void adminReplyListIsPaginated() {
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/replies").session(adminSession).exchange())
                .hasStatusOk()
                .bodyText()
                .contains("Page 1 of", "page=2");
        assertThat(mvc.get().uri("/admin/replies?page=2").session(adminSession).exchange())
                .hasStatusOk()
                .bodyText()
                .contains("Page 2 of", "page=1");
        assertThat(mvc.get()
                        .uri("/admin/replies?page=invalid")
                        .session(adminSession)
                        .exchange())
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    private String status(long replyId) {
        return jdbcClient
                .sql("select status from replies where id = :id")
                .param("id", replyId)
                .query(String.class)
                .single();
    }

    private MvcTestResult deleteAsAdmin(MockHttpSession session, Long replyId) {
        return mvc.post()
                .uri("/admin/replies/{id}/delete", replyId)
                .session(session)
                .with(csrf())
                .exchange();
    }
}
