package dev.sivalabs.feedbackhub.messages.domain.models;

public enum MessageSentiment {
    NEUTRAL("Neutral"),
    HAPPY("Happy"),
    SAD("Sad"),
    ANGRY("Angry"),
    DISAPPOINTED("Disappointed");

    private final String displayName;

    MessageSentiment(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
