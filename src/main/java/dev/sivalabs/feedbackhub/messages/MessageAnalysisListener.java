package dev.sivalabs.feedbackhub.messages;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "feedbackhub.message-analysis.enabled", havingValue = "true", matchIfMissing = true)
class MessageAnalysisListener {
    private final MessageService messageService;

    MessageAnalysisListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @ApplicationModuleListener
    void on(MessageCreatedEvent event) {
        messageService.analyzeMessage(event.messageId());
    }
}
