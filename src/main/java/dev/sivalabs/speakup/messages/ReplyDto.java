package dev.sivalabs.speakup.messages;

import java.time.Instant;

record ReplyDto(
        Long id,
        String visibleAuthor,
        String content,
        Instant createdAt,
        Instant updatedAt,
        long upvoteCount,
        long downvoteCount,
        String currentUserVote,
        boolean deleted) {}
