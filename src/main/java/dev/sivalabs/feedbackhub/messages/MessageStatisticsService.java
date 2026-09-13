package dev.sivalabs.feedbackhub.messages;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageStatisticsService {
    private final MessageRepository messageRepository;
    private final ReplyRepository replyRepository;

    MessageStatisticsService(MessageRepository messageRepository, ReplyRepository replyRepository) {
        this.messageRepository = messageRepository;
        this.replyRepository = replyRepository;
    }

    @Transactional(readOnly = true)
    public MessageStatistics getStatistics() {
        return new MessageStatistics(messageRepository.count(), replyRepository.count());
    }

    public record MessageStatistics(long messageCount, long replyCount) {}
}
