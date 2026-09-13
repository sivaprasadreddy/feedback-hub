package dev.sivalabs.feedbackhub.messages;

import java.time.Instant;

record AdminMessageDto(Long id, String visibleAuthor, String content, Instant createdAt, boolean deleted) {}
