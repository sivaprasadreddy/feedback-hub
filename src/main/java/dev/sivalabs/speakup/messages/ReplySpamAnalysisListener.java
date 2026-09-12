package dev.sivalabs.speakup.messages;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "speakup.reply-spam-analysis.enabled", havingValue = "true", matchIfMissing = true)
class ReplySpamAnalysisListener {
    private final ReplySpamAnalyzer replySpamAnalyzer;
    private final ReplyRepository replyRepository;

    ReplySpamAnalysisListener(ReplySpamAnalyzer replySpamAnalyzer, ReplyRepository replyRepository) {
        this.replySpamAnalyzer = replySpamAnalyzer;
        this.replyRepository = replyRepository;
    }

    @ApplicationModuleListener
    void on(ReplyCreatedEvent event) {
        var analysis = replySpamAnalyzer.analyze(event.content());
        if (analysis == null) {
            throw new IllegalStateException("Reply spam analysis did not return a classification");
        }
        var reply = replyRepository.findById(event.replyId()).orElseThrow();
        reply.setSpam(analysis.spam());
    }
}
