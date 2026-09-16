package dev.sivalabs.feedbackhub.messages;

import java.time.Instant;
import java.util.Set;

record AdminMessageDto(
        Long id,
        String visibleAuthor,
        String content,
        Instant createdAt,
        boolean deleted,
        Set<String> topics,
        MessageSentiment sentiment) {
    public boolean analyzed() {
        return sentiment != null;
    }
}
