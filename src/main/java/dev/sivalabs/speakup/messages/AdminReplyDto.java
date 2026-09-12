package dev.sivalabs.speakup.messages;

import java.time.Instant;

record AdminReplyDto(
        Long id, Long messageId, String visibleAuthor, String content, Instant createdAt, boolean deleted) {}
