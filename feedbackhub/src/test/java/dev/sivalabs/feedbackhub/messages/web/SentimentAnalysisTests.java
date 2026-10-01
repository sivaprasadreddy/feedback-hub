package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sivalabs.feedbackhub.BaseIT;
import org.junit.jupiter.api.Test;

class SentimentAnalysisTests extends BaseIT {
    @Test
    void showsAllSentimentCountsForSelectedInclusiveDateRange() {
        // seeded: 2 HAPPY + 1 ANGRY on 2020-03-10 and 1 SAD on 2020-03-09
        var result = mvc.get()
                .uri("/admin/sentiment-analysis?startDate=2020-03-10&endDate=2020-03-10")
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
}
