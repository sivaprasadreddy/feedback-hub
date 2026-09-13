package dev.sivalabs.feedbackhub.messages;

import java.util.LinkedHashSet;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "feedbackhub.message-analysis.enabled", havingValue = "true", matchIfMissing = true)
class MessageAnalysisListener {
    private static final int MAX_TOPICS = 3;
    private static final int MAX_TOPIC_LENGTH = 40;

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
                    .filter(topic -> topic != null && !topic.isBlank())
                    .map(MessageAnalysisListener::normalizeTopic)
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

    private static String normalizeTopic(String topic) {
        var normalized = topic.trim().replaceAll("\\s+", " ");
        normalized = normalized.substring(0, Math.min(normalized.length(), MAX_TOPIC_LENGTH));
        if (normalized.equalsIgnoreCase("HR")) {
            return "HR";
        }
        return java.util.Arrays.stream(normalized.split(" "))
                .map(word -> word.isEmpty()
                        ? word
                        : word.substring(0, 1).toUpperCase(Locale.ROOT)
                                + word.substring(1).toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
