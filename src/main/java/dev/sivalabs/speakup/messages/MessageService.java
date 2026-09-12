package dev.sivalabs.speakup.messages;

import dev.sivalabs.speakup.users.UserEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MessageService {
    private final MessageRepository messageRepository;
    private final EntityManager entityManager;

    MessageService(MessageRepository messageRepository, EntityManager entityManager) {
        this.messageRepository = messageRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void createMessage(CreateMessageCmd cmd) {
        var message = new MessageEntity();
        message.setContent(cmd.content());
        message.setCreator(entityManager.getReference(UserEntity.class, cmd.creatorId()));
        message.setAnonymous(cmd.anonymous());
        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> findRecentMessages() {
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(message -> new MessageDto(
                        message.getId(),
                        message.isAnonymous()
                                ? "Anonymous"
                                : message.getCreator().getName(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
    }
}
