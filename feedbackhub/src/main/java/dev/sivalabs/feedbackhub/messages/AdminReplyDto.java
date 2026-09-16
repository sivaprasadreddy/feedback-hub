package dev.sivalabs.feedbackhub.messages;

import java.time.Instant;

record AdminReplyDto(
        Long id,
        Long messageId,
        String visibleAuthor,
        String content,
        Instant createdAt,
        boolean deleted,
        boolean spam) {}
