package dev.sivalabs.feedbackhub.messages;

import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "feedbackhub.message-analysis.enabled", havingValue = "true", matchIfMissing = true)
class MessageAnalysisListener {
    private static final int MAX_TOPICS = 3;
    private final MessageAnalyzer messageAnalyzer;
    private final MessageRepository messageRepository;

    MessageAnalysisListener(MessageAnalyzer messageAnalyzer, MessageRepository messageRepository) {
        this.messageAnalyzer = messageAnalyzer;
        this.messageRepository = messageRepository;
    }

    @ApplicationModuleListener
    void on(MessageCreatedEvent event) {
        var analysis = messageAnalyzer.analyze(event.content());
        if (analysis == null || analysis.sentiment() == null) {
            throw new IllegalStateException("Message analysis did not return a sentiment");
        }
        var topics = new LinkedHashSet<String>();
        if (analysis.topics() != null) {
            analysis.topics().stream()
                    .filter(Objects::nonNull)
                    .map(MessageTopic::displayName)
                    .distinct()
                    .limit(MAX_TOPICS)
                    .forEach(topics::add);
        }
        if (topics.isEmpty()) {
            topics.add("Other");
        }
        var message = messageRepository.findById(event.messageId()).orElseThrow();
        message.setTopics(topics);
        message.setSentiment(analysis.sentiment());
    }
}
