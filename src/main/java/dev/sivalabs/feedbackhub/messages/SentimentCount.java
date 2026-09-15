package dev.sivalabs.feedbackhub.messages;

public record SentimentCount(MessageSentiment sentiment, long count, int percentage) {
    public String displayName() {
        return sentiment.getDisplayName();
    }
}
