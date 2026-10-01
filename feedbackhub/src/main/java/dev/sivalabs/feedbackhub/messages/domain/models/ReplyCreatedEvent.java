package dev.sivalabs.feedbackhub.messages.domain.models;

public record ReplyCreatedEvent(Long replyId, String content) {}
