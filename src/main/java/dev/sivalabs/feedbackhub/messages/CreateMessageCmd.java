package dev.sivalabs.feedbackhub.messages;

record CreateMessageCmd(String content, Long creatorId, boolean anonymous) {}
