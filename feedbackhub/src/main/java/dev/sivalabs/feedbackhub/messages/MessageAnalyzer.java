package dev.sivalabs.feedbackhub.messages;

interface MessageAnalyzer {
    MessageAnalysis analyze(String content);
}
