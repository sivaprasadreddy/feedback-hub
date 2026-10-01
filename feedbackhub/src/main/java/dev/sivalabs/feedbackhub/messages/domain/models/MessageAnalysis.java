package dev.sivalabs.feedbackhub.messages.domain.models;

import java.util.Set;

public record MessageAnalysis(Set<MessageTopic> topics, MessageSentiment sentiment) {}
