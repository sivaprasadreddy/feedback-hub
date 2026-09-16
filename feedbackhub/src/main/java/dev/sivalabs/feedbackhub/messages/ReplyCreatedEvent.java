package dev.sivalabs.feedbackhub.messages;

public record ReplyCreatedEvent(Long replyId, String content) {}
