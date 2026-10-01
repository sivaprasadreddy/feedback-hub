package dev.sivalabs.feedbackhub.messages.domain.models;

public record CreateMessageCmd(String content, Long creatorId, boolean anonymous) {}
