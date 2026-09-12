package dev.sivalabs.speakup.messages;

record CreateMessageCmd(String content, Long creatorId, boolean anonymous) {}
