package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.ReplySpamAnalyzer;
import dev.sivalabs.feedbackhub.messages.domain.models.ReplyCreatedEvent;
import dev.sivalabs.feedbackhub.messages.domain.models.ReplySpamAnalysis;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@RecordApplicationEvents
@TestPropertySource(properties = "feedbackhub.reply-spam-analysis.enabled=true")
class ReplySpamAnalysisTests extends BaseIT {
    // seeded: message 4201 by siva
    private static final long MESSAGE_ID = 4201L;

    @Autowired
    ApplicationEvents applicationEvents;

    @MockitoBean
    ReplySpamAnalyzer replySpamAnalyzer;

    @Test
    void postedSpamReplyIsTaggedForAdminReview() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var messageId = MESSAGE_ID;

        var replyContent = "Buy discounted gift cards at an unrelated promotional link " + UUID.randomUUID();
        when(replySpamAnalyzer.analyze(replyContent)).thenReturn(new ReplySpamAnalysis(true));
        assertThat(mvc.post()
                        .uri("/messages/{id}/replies", messageId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", replyContent)
                        .param("postingIdentity", "ANONYMOUS")
                        .session(userSession)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + messageId);

        var createdEvent = applicationEvents.stream(ReplyCreatedEvent.class)
                .filter(event -> event.content().equals(replyContent))
                .findFirst()
                .orElseThrow();
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(jdbcClient
                                .sql("select spam from replies where id = :id")
                                .param("id", createdEvent.replyId())
                                .query(Boolean.class)
                                .single())
                        .isTrue());

        var adminSession = session(login("admin@gmail.com", "secret"));
        assertThat(mvc.get().uri("/admin/replies").session(adminSession).exchange())
                .bodyText()
                .contains(replyContent, "Spam", "Delete reply");
        assertThat(mvc.get()
                        .uri("/messages/{id}", messageId)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(replyContent)
                .doesNotContain("Spam");
    }
}
