package dev.sivalabs.feedbackhub.messages;

import java.util.Set;

record MessageAnalysis(Set<MessageTopic> topics, MessageSentiment sentiment) {}
