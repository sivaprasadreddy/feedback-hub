package dev.sivalabs.feedbackhub.messages.domain;

import dev.sivalabs.feedbackhub.messages.domain.models.ReplySpamAnalysis;

public interface ReplySpamAnalyzer {
    ReplySpamAnalysis analyze(String content);
}
