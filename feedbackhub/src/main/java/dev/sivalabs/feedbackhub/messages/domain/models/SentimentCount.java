package dev.sivalabs.feedbackhub.messages.domain.models;

public record SentimentCount(MessageSentiment sentiment, long count, int percentage) {
    public String displayName() {
        return sentiment.getDisplayName();
    }
}
