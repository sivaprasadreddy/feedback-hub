package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageAnalyzer;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import dev.sivalabs.feedbackhub.messages.domain.models.MessageAnalysis;
import dev.sivalabs.feedbackhub.messages.domain.models.MessageSentiment;
import dev.sivalabs.feedbackhub.messages.domain.models.MessageTopic;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ModerateMessageTests extends BaseIT {
    private static final long DELETE_ID = 5001L;
    private static final String DELETE_CONTENT = "Moderation admin-deleted message";
    private static final long ANONYMOUS_ID = 5002L;
    private static final String ANONYMOUS_CONTENT = "Moderation anonymous admin deletion";
    private static final long PROTECTED_ID = 5003L;
    private static final long REVIEW_ID = 5004L;
    private static final String REVIEW_CONTENT = "Moderation review target";
    private static final long ANALYZE_ID = 5005L;
    private static final String ANALYZE_CONTENT = "Moderation analyze from admin";
    private static final long FAILED_ANALYSIS_ID = 5006L;
    private static final String FAILED_ANALYSIS_CONTENT = "Moderation failed admin analysis";

    @MockitoBean
    MessageAnalyzer messageAnalyzer;

    @Test
    void adminCanDeleteAnotherUsersMessageWithAuditAndAssociationsPreserved() {
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/delete", DELETE_ID)
                        .session(adminSession)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/admin/messages");

        entityManager.flush();
        assertThat(status(DELETE_ID)).isEqualTo("DELETED");
        assertThat(jdbcClient
                        .sql("select deleted_by_admin_user_id from messages where id = :id")
                        .param("id", DELETE_ID)
                        .query(Long.class)
                        .single())
                .isEqualTo(1L);
        assertThat(jdbcClient
                        .sql("select deleted_by_admin_at is not null from messages where id = :id")
                        .param("id", DELETE_ID)
                        .query(Boolean.class)
                        .single())
                .isTrue();
        assertThat(jdbcClient
                        .sql("select count(*) from replies where message_id = :id and status = 'ACTIVE'")
                        .param("id", DELETE_ID)
                        .query(Long.class)
                        .single())
                .isOne();
    }

    @Test
    void adminDeletedAnonymousMessageShowsPlaceholderWithoutExposingCreator() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var adminSession = session(login("admin@gmail.com", "secret"));
        deleteAsAdmin(adminSession, ANONYMOUS_ID);

        assertThat(mvc.get()
                        .uri("/messages/{id}", ANONYMOUS_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Anonymous", MessageService.DELETED_CONTENT)
                .doesNotContain(ANONYMOUS_CONTENT, "siva@gmail.com");
    }

    @Test
    void regularAndUnauthenticatedUsersCannotAdministrativelyDelete() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(deleteAsAdmin(userSession, PROTECTED_ID)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/delete", PROTECTED_ID)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        entityManager.flush();
        assertThat(status(PROTECTED_ID)).isEqualTo("ACTIVE");
    }

    @Test
    void adminCanReviewMessagesAndCannotDeleteDeletedMessageAgain() {
        var adminSession = session(login("admin@gmail.com", "secret"));

        assertThat(mvc.get().uri("/admin/messages").session(adminSession).exchange())
                .hasStatusOk()
                .hasViewName("admin/messages")
                .bodyText()
                .contains(REVIEW_CONTENT, "Delete message");
        deleteAsAdmin(adminSession, REVIEW_ID);
        assertThat(deleteAsAdmin(adminSession, REVIEW_ID))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
    }

    @Test
    void adminCanAnalyzeUnanalyzedMessageWithHtmx() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        when(messageAnalyzer.analyze(ANALYZE_CONTENT))
                .thenReturn(new MessageAnalysis(
                        Set.of(MessageTopic.WORK_CULTURE, MessageTopic.PEOPLE_AND_TEAM), MessageSentiment.HAPPY));

        assertThat(mvc.get().uri("/admin/messages").session(adminSession).exchange())
                .bodyText()
                .contains(
                        ANALYZE_CONTENT,
                        "Analyze message",
                        "hx-post=\"/admin/messages/" + ANALYZE_ID + "/analyze\"",
                        "hx-target=\"closest .admin-message\"",
                        "hx-swap=\"outerHTML\"");

        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/analyze", ANALYZE_ID)
                        .header("HX-Request", "true")
                        .session(adminSession)
                        .with(csrf())
                        .exchange())
                .hasStatusOk()
                .hasViewName("fragments/admin-message :: message(message=${message})")
                .bodyText()
                .contains(ANALYZE_CONTENT, "Happy", "Work Culture", "People &amp; Team")
                .doesNotContain("Analyze message", "<html");

        entityManager.flush();
        assertThat(jdbcClient
                        .sql("select sentiment from messages where id = :id")
                        .param("id", ANALYZE_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("HAPPY");
        assertThat(topics(ANALYZE_ID)).containsExactlyInAnyOrder("Work Culture", "People & Team");
    }

    @Test
    void analysisErrorKeepsPreviousMessageDetailsAndShowsRetryOption() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        when(messageAnalyzer.analyze(FAILED_ANALYSIS_CONTENT)).thenThrow(new IllegalStateException("AI unavailable"));

        assertThat(mvc.post()
                        .uri("/admin/messages/{id}/analyze", FAILED_ANALYSIS_ID)
                        .header("HX-Request", "true")
                        .session(adminSession)
                        .with(csrf())
                        .exchange())
                .hasStatusOk()
                .bodyText()
                .contains(
                        FAILED_ANALYSIS_CONTENT,
                        "Something went wrong while analyzing this message. Please try again.",
                        "Analyze message",
                        "Delete message")
                .doesNotContain("Internal Server Error", "<html");

        entityManager.flush();
        assertThat(jdbcClient
                        .sql("select sentiment from messages where id = :id")
                        .param("id", FAILED_ANALYSIS_ID)
                        .query(String.class)
                        .optional())
                .isEmpty();
        assertThat(topics(FAILED_ANALYSIS_ID)).isEmpty();
    }

    @Test
    void adminMessageListIsPaginated() {
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

    private String status(long messageId) {
        return jdbcClient
                .sql("select status from messages where id = :id")
                .param("id", messageId)
                .query(String.class)
                .single();
    }

    private List<String> topics(long messageId) {
        return jdbcClient
                .sql("select topic from message_topics where message_id = :id")
                .param("id", messageId)
                .query(String.class)
                .list();
    }

    private MvcTestResult deleteAsAdmin(MockHttpSession session, Long messageId) {
        return mvc.post()
                .uri("/admin/messages/{id}/delete", messageId)
                .session(session)
                .with(csrf())
                .exchange();
    }
}
