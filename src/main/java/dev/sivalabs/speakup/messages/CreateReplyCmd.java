package dev.sivalabs.speakup.messages;

record CreateReplyCmd(Long messageId, String content, Long creatorId, boolean anonymous) {}
