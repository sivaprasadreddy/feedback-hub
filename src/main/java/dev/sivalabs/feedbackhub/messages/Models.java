package dev.sivalabs.feedbackhub.messages;

import java.util.List;
import java.util.Set;

record CreateMessageCmd(String content, Long creatorId, boolean anonymous) {}

record CreateReplyCmd(Long messageId, String content, Long creatorId, boolean anonymous) {}

record MessageAnalysis(Set<MessageTopic> topics, MessageSentiment sentiment) {}

record ReplySpamAnalysis(boolean spam) {}

record MessageExportData(AdminMessageDto message, List<AdminReplyDto> replies) {}
