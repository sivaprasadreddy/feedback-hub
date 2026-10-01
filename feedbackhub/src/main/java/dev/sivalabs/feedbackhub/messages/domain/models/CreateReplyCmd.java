package dev.sivalabs.feedbackhub.messages.domain.models;

public record CreateReplyCmd(Long messageId, String content, Long creatorId, boolean anonymous) {}
