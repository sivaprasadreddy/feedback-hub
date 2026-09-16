package dev.sivalabs.feedbackhub.messages;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sivalabs.feedbackhub.BaseIT;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SentimentAnalysisTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Test
    void showsAllSentimentCountsForSelectedInclusiveDateRange() {
        var selectedDate = LocalDate.of(2026, 9, 10);
        createMessage(MessageSentiment.HAPPY, selectedDate);
        createMessage(MessageSentiment.HAPPY, selectedDate);
        createMessage(MessageSentiment.ANGRY, selectedDate);
        createMessage(MessageSentiment.SAD, selectedDate.minusDays(1));

        var result = mvc.get()
                .uri("/admin/sentiment-analysis?startDate=2026-09-10&endDate=2026-09-10")
                .session(session(login("admin@gmail.com", "secret")))
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .hasViewName("admin/sentiment-analysis")
                .bodyText()
                .contains("Happy: 2 messages", "Angry: 1 messages", "Neutral: 0 messages", "Sad: 0 messages")
                .doesNotContain("Sad: 1 messages");
    }

    @Test
    void rejectsAnInvertedDateRange() {
        var result = mvc.get()
                .uri("/admin/sentiment-analysis?startDate=2026-09-11&endDate=2026-09-10")
                .session(session(login("admin@gmail.com", "secret")))
                .exchange();

        assertThat(result).hasStatusOk().bodyText().contains("Start date must be on or before end date.");
    }

    private void createMessage(MessageSentiment sentiment, LocalDate date) {
        var message = new MessageEntity();
        message.setContent("Sentiment analysis test message");
        message.setCreatorUserId(2L);
        message.setAnonymous(false);
        message.setSentiment(sentiment);
        messageRepository.saveAndFlush(message);
        message.setCreatedAt(date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant());
        messageRepository.flush();
    }
}
