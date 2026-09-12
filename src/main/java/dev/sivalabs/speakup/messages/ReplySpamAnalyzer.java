package dev.sivalabs.speakup.messages;

interface ReplySpamAnalyzer {
    ReplySpamAnalysis analyze(String content);
}
