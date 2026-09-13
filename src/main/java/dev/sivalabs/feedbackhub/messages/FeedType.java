package dev.sivalabs.feedbackhub.messages;

import dev.sivalabs.feedbackhub.shared.BadRequestException;
import java.util.Arrays;

enum FeedType {
    RECENT("recent"),
    POPULAR("popular");

    private final String value;

    FeedType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    static FeedType fromValue(String value) {
        return Arrays.stream(values())
                .filter(feedType -> feedType.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown feed: " + value));
    }
}
