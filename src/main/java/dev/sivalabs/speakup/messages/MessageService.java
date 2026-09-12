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
    static final String DELETED_REPLY_CONTENT = "This reply has been deleted.";
    private final MessageRepository messageRepository;
    private final ReplyRepository replyRepository;
    private final MessageVoteRepository messageVoteRepository;
    private final ReplyVoteRepository replyVoteRepository;
    private final EntityManager entityManager;

    MessageService(
            MessageRepository messageRepository,
            ReplyRepository replyRepository,
            MessageVoteRepository messageVoteRepository,
            ReplyVoteRepository replyVoteRepository,
            EntityManager entityManager) {
        this.messageRepository = messageRepository;
        this.replyRepository = replyRepository;
        this.messageVoteRepository = messageVoteRepository;
        this.replyVoteRepository = replyVoteRepository;
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
    public List<MessageDto> findRecentMessages(Long currentUserId) {
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(message -> new MessageDto(
                        message.getId(),
                        message.isAnonymous()
                                ? "Anonymous"
                                : message.getCreator().getName(),
                        message.getStatus() == MessageStatus.DELETED ? DELETED_CONTENT : message.getContent(),
                        message.getCreatedAt(),
                        messageVoteRepository.countByMessageIdAndVoteType(message.getId(), VoteType.UPVOTE),
                        messageVoteRepository.countByMessageIdAndVoteType(message.getId(), VoteType.DOWNVOTE),
                        replyRepository.countByMessageIdAndStatus(message.getId(), ReplyStatus.ACTIVE),
                        messageVoteRepository
                                .findByMessageIdAndVoterId(message.getId(), currentUserId)
                                .map(MessageVoteEntity::getVoteType)
                                .map(Enum::name)
                                .orElse(null),
                        message.getStatus() == MessageStatus.DELETED))
                .toList();
    }

    @Transactional(readOnly = true)
    public MessageDetailsDto findMessage(Long messageId, Long currentUserId) {
        var message = messageRepository
                .findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        var deleted = message.getStatus() == MessageStatus.DELETED;
        var ownedByCurrentUser = message.getCreator().getId().equals(currentUserId);
        var currentUserVote = messageVoteRepository
                .findByMessageIdAndVoterId(messageId, currentUserId)
                .map(MessageVoteEntity::getVoteType)
                .map(Enum::name)
                .orElse(null);
        return new MessageDetailsDto(
                message.getId(),
                message.isAnonymous() ? "Anonymous" : message.getCreator().getName(),
                deleted ? DELETED_CONTENT : message.getContent(),
                message.getCreatedAt(),
                messageVoteRepository.countByMessageIdAndVoteType(messageId, VoteType.UPVOTE),
                messageVoteRepository.countByMessageIdAndVoteType(messageId, VoteType.DOWNVOTE),
                replyRepository.countByMessageIdAndStatus(messageId, ReplyStatus.ACTIVE),
                currentUserVote,
                deleted,
                !deleted && ownedByCurrentUser,
                !deleted && !ownedByCurrentUser);
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

    @Transactional(readOnly = true)
    public List<ReplyDto> findReplies(Long messageId, Long currentUserId) {
        if (!messageRepository.existsById(messageId)) {
            throw new ResourceNotFoundException("Message not found");
        }
        return replyRepository.findAllByMessageIdOrderByCreatedAtAsc(messageId).stream()
                .map(reply -> {
                    var deleted = reply.getStatus() == ReplyStatus.DELETED;
                    return new ReplyDto(
                            reply.getId(),
                            reply.isAnonymous()
                                    ? "Anonymous"
                                    : reply.getCreator().getName(),
                            deleted ? DELETED_REPLY_CONTENT : reply.getContent(),
                            reply.getCreatedAt(),
                            reply.getUpdatedAt(),
                            replyVoteRepository.countByReplyIdAndVoteType(reply.getId(), VoteType.UPVOTE),
                            replyVoteRepository.countByReplyIdAndVoteType(reply.getId(), VoteType.DOWNVOTE),
                            replyVoteRepository
                                    .findByReplyIdAndVoterId(reply.getId(), currentUserId)
                                    .map(ReplyVoteEntity::getVoteType)
                                    .map(Enum::name)
                                    .orElse(null),
                            deleted,
                            !deleted && reply.getCreator().getId().equals(currentUserId),
                            !deleted && !reply.getCreator().getId().equals(currentUserId));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public EditReplyForm getReplyEditForm(Long messageId, Long replyId, Long currentUserId) {
        var reply = getEditableReply(messageId, replyId, currentUserId);
        return new EditReplyForm(reply.getContent());
    }

    @Transactional(readOnly = true)
    public void validateReplyCanBeEdited(Long messageId, Long replyId, Long currentUserId) {
        getEditableReply(messageId, replyId, currentUserId);
    }

    @Transactional
    public void editReply(Long messageId, Long replyId, Long currentUserId, String content) {
        var reply = getEditableReply(messageId, replyId, currentUserId);
        reply.setContent(content);
    }

    @Transactional
    public void deleteReply(Long messageId, Long replyId, Long currentUserId) {
        var reply = getEditableReply(messageId, replyId, currentUserId);
        reply.setStatus(ReplyStatus.DELETED);
    }

    @Transactional
    public void voteOnMessage(Long messageId, Long currentUserId, VoteType voteType) {
        var message = getVotableMessage(messageId, currentUserId);
        var vote = messageVoteRepository
                .findByMessageIdAndVoterId(messageId, currentUserId)
                .orElseGet(() -> {
                    var newVote = new MessageVoteEntity();
                    newVote.setMessage(message);
                    newVote.setVoter(entityManager.getReference(UserEntity.class, currentUserId));
                    return newVote;
                });
        vote.setVoteType(voteType);
        messageVoteRepository.save(vote);
    }

    @Transactional
    public void removeMessageVote(Long messageId, Long currentUserId) {
        getVotableMessage(messageId, currentUserId);
        messageVoteRepository
                .findByMessageIdAndVoterId(messageId, currentUserId)
                .ifPresent(messageVoteRepository::delete);
    }

    @Transactional
    public void voteOnReply(Long messageId, Long replyId, Long currentUserId, VoteType voteType) {
        var reply = getVotableReply(messageId, replyId, currentUserId);
        var vote = replyVoteRepository
                .findByReplyIdAndVoterId(replyId, currentUserId)
                .orElseGet(() -> {
                    var newVote = new ReplyVoteEntity();
                    newVote.setReply(reply);
                    newVote.setVoter(entityManager.getReference(UserEntity.class, currentUserId));
                    return newVote;
                });
        vote.setVoteType(voteType);
        replyVoteRepository.save(vote);
    }

    @Transactional
    public void removeReplyVote(Long messageId, Long replyId, Long currentUserId) {
        getVotableReply(messageId, replyId, currentUserId);
        replyVoteRepository.findByReplyIdAndVoterId(replyId, currentUserId).ifPresent(replyVoteRepository::delete);
    }

    private ReplyEntity getVotableReply(Long messageId, Long replyId, Long currentUserId) {
        var reply = replyRepository
                .findById(replyId)
                .filter(candidate -> candidate.getMessage().getId().equals(messageId))
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found"));
        if (reply.getStatus() == ReplyStatus.DELETED) {
            throw new AccessDeniedException("Deleted replies cannot be voted on");
        }
        if (reply.getCreator().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You cannot vote on your own reply");
        }
        return reply;
    }

    private MessageEntity getVotableMessage(Long messageId, Long currentUserId) {
        var message = messageRepository
                .findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (message.getStatus() == MessageStatus.DELETED) {
            throw new AccessDeniedException("Deleted messages cannot be voted on");
        }
        if (message.getCreator().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You cannot vote on your own message");
        }
        return message;
    }

    private ReplyEntity getEditableReply(Long messageId, Long replyId, Long currentUserId) {
        var reply = replyRepository
                .findById(replyId)
                .filter(candidate -> candidate.getMessage().getId().equals(messageId))
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found"));
        if (!reply.getCreator().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own replies");
        }
        if (reply.getStatus() == ReplyStatus.DELETED) {
            throw new AccessDeniedException("Deleted replies cannot be edited");
        }
        return reply;
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
