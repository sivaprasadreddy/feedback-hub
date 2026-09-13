package dev.sivalabs.feedbackhub.messages;

enum MessageSentiment {
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
