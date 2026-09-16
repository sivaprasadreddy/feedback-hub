package dev.sivalabs.feedbackhub.messages;

interface ReplySpamAnalyzer {
    ReplySpamAnalysis analyze(String content);
}
