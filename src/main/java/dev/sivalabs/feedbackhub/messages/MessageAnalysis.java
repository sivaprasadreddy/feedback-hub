package dev.sivalabs.feedbackhub.messages;

import java.util.Set;

record MessageAnalysis(Set<String> topics, MessageSentiment sentiment) {}
