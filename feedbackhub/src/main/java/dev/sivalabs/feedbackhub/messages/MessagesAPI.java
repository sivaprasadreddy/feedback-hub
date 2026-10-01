package dev.sivalabs.feedbackhub.messages;

import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import dev.sivalabs.feedbackhub.messages.domain.models.MessageStatistics;
import org.springframework.stereotype.Component;

@Component
public class MessagesAPI {
    private final MessageService messageService;

    MessagesAPI(MessageService messageService) {
        this.messageService = messageService;
    }

    public MessageStatistics getStatistics() {
        return messageService.getStatistics();
    }
}
