package dev.sivalabs.speakup.messages;

import java.time.Instant;
import java.util.Set;

record MessageDetailsDto(
        Long id,
        String visibleAuthor,
        String content,
        Instant createdAt,
        long upvoteCount,
        long downvoteCount,
        long replyCount,
        String currentUserVote,
        boolean deleted,
        boolean editable,
        boolean votable,
        Set<String> topics,
        MessageSentiment sentiment) {}
