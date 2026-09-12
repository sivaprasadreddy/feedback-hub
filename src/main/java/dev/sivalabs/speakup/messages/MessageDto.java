package dev.sivalabs.speakup.messages;

import java.time.Instant;

record MessageDto(Long id, String visibleAuthor, String content, Instant createdAt) {}
