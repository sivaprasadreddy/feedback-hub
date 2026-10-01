package dev.sivalabs.feedbackhub.messages.domain.models;

import java.time.Instant;

public record ReplyDto(
        Long id,
        String visibleAuthor,
        String content,
        Instant createdAt,
        Instant updatedAt,
        long upvoteCount,
        long downvoteCount,
        String currentUserVote,
        boolean deleted,
        boolean editable,
        boolean votable) {}
