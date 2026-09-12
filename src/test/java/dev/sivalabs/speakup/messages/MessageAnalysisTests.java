package dev.sivalabs.speakup.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.speakup.BaseIT;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@RecordApplicationEvents
@TestPropertySource(properties = "speakup.message-analysis.enabled=true")
class MessageAnalysisTests extends BaseIT {
    @Autowired
    MessageService messageService;

    @Autowired
    ApplicationEvents applicationEvents;

    @MockitoBean
    MessageAnalyzer messageAnalyzer;

    @Test
    void postedMessageIsAnalyzedFromEventAndAnalysisIsDisplayed() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = "I am delighted with our new learning budget " + UUID.randomUUID();
        when(messageAnalyzer.analyze(content))
                .thenReturn(new MessageAnalysis(
                        Set.of(" Employee Benefits ", "Learning", "learning", "A".repeat(50)), MessageSentiment.HAPPY));

        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", "IDENTIFIED")
                        .session(userSession)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/");

        var createdEvent = applicationEvents.stream(MessageCreatedEvent.class)
                .filter(event -> event.content().equals(content))
                .findFirst()
                .orElseThrow();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var message = messageService.findMessage(createdEvent.messageId(), 2L);
            assertThat(message.sentiment()).isEqualTo(MessageSentiment.HAPPY);
            assertThat(message.labels()).containsExactlyInAnyOrder("employee benefits", "learning", "a".repeat(40));
        });

        assertThat(mvc.get().uri("/").session(userSession).exchange())
                .bodyText()
                .contains(content, "Happy", "employee benefits", "learning");
        assertThat(mvc.get()
                        .uri("/messages/{id}", createdEvent.messageId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(content, "Happy", "employee benefits", "learning");
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
