package dev.sivalabs.feedbackhub.messages.domain.models;

public record MessageCreatedEvent(Long messageId, String content) {}
