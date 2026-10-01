package dev.sivalabs.feedbackhub.messages.domain.models;

import java.time.Instant;

public record AdminReplyDto(
        Long id,
        Long messageId,
        String visibleAuthor,
        String content,
        Instant createdAt,
        boolean deleted,
        boolean spam) {}
