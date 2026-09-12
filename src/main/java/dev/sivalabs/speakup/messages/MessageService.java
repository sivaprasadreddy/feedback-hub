package dev.sivalabs.speakup.messages;

import dev.sivalabs.speakup.shared.ResourceNotFoundException;
import dev.sivalabs.speakup.users.UserEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MessageService {
    static final String DELETED_CONTENT = "This message has been deleted.";
    private final MessageRepository messageRepository;
    private final ReplyRepository replyRepository;
    private final EntityManager entityManager;

    MessageService(MessageRepository messageRepository, ReplyRepository replyRepository, EntityManager entityManager) {
        this.messageRepository = messageRepository;
        this.replyRepository = replyRepository;
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
                        message.getStatus() == MessageStatus.DELETED ? DELETED_CONTENT : message.getContent(),
                        message.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MessageDetailsDto findMessage(Long messageId, Long currentUserId) {
        var message = messageRepository
                .findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        var deleted = message.getStatus() == MessageStatus.DELETED;
        return new MessageDetailsDto(
                message.getId(),
                message.isAnonymous() ? "Anonymous" : message.getCreator().getName(),
                deleted ? DELETED_CONTENT : message.getContent(),
                message.getCreatedAt(),
                0,
                0,
                replyRepository.countByMessageId(messageId),
                null,
                deleted,
                !deleted && message.getCreator().getId().equals(currentUserId));
    }

    @Transactional(readOnly = true)
    public EditMessageForm getEditForm(Long messageId, Long currentUserId) {
        var message = getEditableMessage(messageId, currentUserId);
        return new EditMessageForm(message.getContent());
    }

    @Transactional
    public void editMessage(Long messageId, Long currentUserId, String content) {
        var message = getEditableMessage(messageId, currentUserId);
        message.setContent(content);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long currentUserId) {
        var message = getEditableMessage(messageId, currentUserId);
        message.setStatus(MessageStatus.DELETED);
    }

    @Transactional
    public void createReply(CreateReplyCmd cmd) {
        var message = messageRepository
                .findById(cmd.messageId())
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (message.getStatus() == MessageStatus.DELETED) {
            throw new AccessDeniedException("Deleted messages cannot be replied to");
        }
        var reply = new ReplyEntity();
        reply.setMessage(message);
        reply.setContent(cmd.content());
        reply.setCreator(entityManager.getReference(UserEntity.class, cmd.creatorId()));
        reply.setAnonymous(cmd.anonymous());
        replyRepository.save(reply);
    }

    private MessageEntity getEditableMessage(Long messageId, Long currentUserId) {
        var message = messageRepository
                .findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (!message.getCreator().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own messages");
        }
        if (message.getStatus() == MessageStatus.DELETED) {
            throw new AccessDeniedException("Deleted messages cannot be edited");
        }
        return message;
    }
}
