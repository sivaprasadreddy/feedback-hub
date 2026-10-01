package dev.sivalabs.feedbackhub.messages.domain;

import dev.sivalabs.feedbackhub.messages.domain.models.MessageAnalysis;

public interface MessageAnalyzer {
    MessageAnalysis analyze(String content);
}
