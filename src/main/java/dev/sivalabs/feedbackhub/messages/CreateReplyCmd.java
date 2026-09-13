package dev.sivalabs.feedbackhub.messages;

record CreateReplyCmd(Long messageId, String content, Long creatorId, boolean anonymous) {}
