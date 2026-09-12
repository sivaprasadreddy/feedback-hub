package dev.sivalabs.speakup.messages;

import java.util.LinkedHashSet;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "speakup.message-analysis.enabled", havingValue = "true", matchIfMissing = true)
class MessageAnalysisListener {
    private static final int MAX_LABELS = 5;
    private static final int MAX_LABEL_LENGTH = 40;

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
        var labels = new LinkedHashSet<String>();
        if (analysis.labels() != null) {
            analysis.labels().stream()
                    .filter(label -> label != null && !label.isBlank())
                    .map(label -> label.trim().toLowerCase(Locale.ROOT))
                    .map(label -> label.substring(0, Math.min(label.length(), MAX_LABEL_LENGTH)))
                    .limit(MAX_LABELS)
                    .forEach(labels::add);
        }
        if (labels.isEmpty()) {
            labels.add("general");
        }
        var message = messageRepository.findById(event.messageId()).orElseThrow();
        message.setLabels(labels);
        message.setSentiment(analysis.sentiment());
    }
}
