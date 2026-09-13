package dev.sivalabs.feedbackhub.messages;

public record MessageCreatedEvent(Long messageId, String content) {}
