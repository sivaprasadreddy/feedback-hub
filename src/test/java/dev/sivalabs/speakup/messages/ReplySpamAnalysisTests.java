package dev.sivalabs.speakup.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.speakup.BaseIT;
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
@TestPropertySource(properties = "speakup.reply-spam-analysis.enabled=true")
class ReplySpamAnalysisTests extends BaseIT {
    @Autowired
    ReplyRepository replyRepository;

    @Autowired
    ApplicationEvents applicationEvents;

    @MockitoBean
    ReplySpamAnalyzer replySpamAnalyzer;

    @Test
    void postedSpamReplyIsTaggedForAdminReview() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var messageContent = "Discussion for spam classification " + UUID.randomUUID();
        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", messageContent)
                        .param("postingIdentity", "IDENTIFIED")
                        .session(userSession)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        var messageId = applicationEvents.stream(MessageCreatedEvent.class)
                .filter(event -> event.content().equals(messageContent))
                .map(MessageCreatedEvent::messageId)
                .findFirst()
                .orElseThrow();

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
                .untilAsserted(() -> assertThat(replyRepository
                                .findById(createdEvent.replyId())
                                .orElseThrow()
                                .isSpam())
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
