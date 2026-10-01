package dev.sivalabs.feedbackhub.messages.domain.models;

import dev.sivalabs.feedbackhub.shared.BadRequestException;
import java.util.Arrays;

public enum FeedType {
    RECENT("recent"),
    POPULAR("popular");

    private final String value;

    FeedType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static FeedType fromValue(String value) {
        return Arrays.stream(values())
                .filter(feedType -> feedType.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown feed: " + value));
    }
}
