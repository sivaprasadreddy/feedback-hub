package dev.sivalabs.feedbackhub.messages;

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
