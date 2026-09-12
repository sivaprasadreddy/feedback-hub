package dev.sivalabs.speakup.messages;

import dev.sivalabs.speakup.shared.PagedResult;
import dev.sivalabs.speakup.shared.ResourceNotFoundException;
import dev.sivalabs.speakup.users.UsersAPI;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MessageService {
    static final int FEED_PAGE_SIZE = 10;
    static final int ADMIN_PAGE_SIZE = 20;
    static final String DELETED_CONTENT = "This message has been deleted.";
    static final String DELETED_REPLY_CONTENT = "This reply has been deleted.";
    private final MessageRepository messageRepository;
    private final ReplyRepository replyRepository;
    private final MessageVoteRepository messageVoteRepository;
    private final ReplyVoteRepository replyVoteRepository;
    private final UsersAPI usersAPI;
    private final ApplicationEventPublisher eventPublisher;

    MessageService(
            MessageRepository messageRepository,
            ReplyRepository replyRepository,
            MessageVoteRepository messageVoteRepository,
            ReplyVoteRepository replyVoteRepository,
            UsersAPI usersAPI,
            ApplicationEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.replyRepository = replyRepository;
        this.messageVoteRepository = messageVoteRepository;
        this.replyVoteRepository = replyVoteRepository;
        this.usersAPI = usersAPI;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void createMessage(CreateMessageCmd cmd) {
        var message = new MessageEntity();
        message.setContent(cmd.content());
        message.setCreatorUserId(cmd.creatorId());
        message.setAnonymous(cmd.anonymous());
        messageRepository.save(message);
        eventPublisher.publishEvent(new MessageCreatedEvent(message.getId(), message.getContent()));
    }

    @Transactional(readOnly = true)
    public PagedResult<MessageDto> findRecentMessages(Long currentUserId, int pageNo) {
        return toMessagePage(messageRepository.findPageByOrderByCreatedAtDesc(feedPageRequest(pageNo)), currentUserId);
    }

    @Transactional(readOnly = true)
    public PagedResult<MessageDto> findPopularMessages(Long currentUserId, int pageNo) {
        return toMessagePage(messageRepository.findAllByPopularity(feedPageRequest(pageNo)), currentUserId);
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminMessageDto> findMessagesForAdmin(int pageNo) {
        var messages = messageRepository.findAllByOrderByCreatedAtDesc(adminPageRequest(pageNo));
        var userNames = getUserNames(messages.getContent());
        return PagedResult.from(messages)
                .map(message -> new AdminMessageDto(
                        message.getId(),
                        message.isAnonymous() ? "Anonymous" : getUserName(message.getCreatorUserId(), userNames),
                        message.getStatus() == MessageStatus.DELETED ? DELETED_CONTENT : message.getContent(),
                        message.getCreatedAt(),
                        message.getStatus() == MessageStatus.DELETED));
    }

    @Transactional
    public void deleteMessageAsAdmin(Long messageId, Long adminId) {
        var message = messageRepository
                .findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (message.getStatus() == MessageStatus.DELETED) {
            throw new AccessDeniedException("Message has already been deleted");
        }
        message.setStatus(MessageStatus.DELETED);
        message.setDeletedByAdminUserId(adminId);
        message.setDeletedByAdminAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminReplyDto> findRepliesForAdmin(int pageNo) {
        var replies = replyRepository.findAllByOrderByCreatedAtDesc(adminPageRequest(pageNo));
        var userNames = getReplyUserNames(replies.getContent());
        return PagedResult.from(replies)
                .map(reply -> new AdminReplyDto(
                        reply.getId(),
                        reply.getMessage().getId(),
                        reply.isAnonymous() ? "Anonymous" : getUserName(reply.getCreatorUserId(), userNames),
                        reply.getStatus() == ReplyStatus.DELETED ? DELETED_REPLY_CONTENT : reply.getContent(),
                        reply.getCreatedAt(),
                        reply.getStatus() == ReplyStatus.DELETED,
                        reply.isSpam()));
    }

    @Transactional
    public void deleteReplyAsAdmin(Long replyId, Long adminId) {
        var reply =
                replyRepository.findById(replyId).orElseThrow(() -> new ResourceNotFoundException("Reply not found"));
        if (reply.getStatus() == ReplyStatus.DELETED) {
            throw new AccessDeniedException("Reply has already been deleted");
        }
        reply.setStatus(ReplyStatus.DELETED);
        reply.setDeletedByAdminUserId(adminId);
        reply.setDeletedByAdminAt(Instant.now());
    }

    private PageRequest feedPageRequest(int pageNo) {
        return PageRequest.of(pageNo - 1, FEED_PAGE_SIZE);
    }

    private PageRequest adminPageRequest(int pageNo) {
        return PageRequest.of(pageNo - 1, ADMIN_PAGE_SIZE);
    }

    private MessageDto toMessageDto(MessageEntity message, Long currentUserId) {
        return new MessageDto(
                message.getId(),
                message.isAnonymous() ? "Anonymous" : getUserName(message.getCreatorUserId()),
                message.getStatus() == MessageStatus.DELETED ? DELETED_CONTENT : message.getContent(),
                message.getCreatedAt(),
                messageVoteRepository.countByMessageIdAndVoteType(message.getId(), VoteType.UPVOTE),
                messageVoteRepository.countByMessageIdAndVoteType(message.getId(), VoteType.DOWNVOTE),
                replyRepository.countByMessageIdAndStatus(message.getId(), ReplyStatus.ACTIVE),
                messageVoteRepository
                        .findByMessageIdAndVoterUserId(message.getId(), currentUserId)
                        .map(MessageVoteEntity::getVoteType)
                        .map(Enum::name)
                        .orElse(null),
                message.getStatus() == MessageStatus.DELETED,
                message.getStatus() != MessageStatus.DELETED
                        && !message.getCreatorUserId().equals(currentUserId),
                new LinkedHashSet<>(message.getLabels()),
                message.getSentiment());
    }

    private PagedResult<MessageDto> toMessagePage(
            org.springframework.data.domain.Page<MessageEntity> page, Long currentUserId) {
        var messages = page.getContent();
        var messageIds = messages.stream().map(MessageEntity::getId).toList();
        if (messageIds.isEmpty()) {
            return PagedResult.from(page).map(message -> toMessageDto(message, currentUserId));
        }
        var counts = messageRepository.findCountsByMessageIds(messageIds).stream()
                .collect(Collectors.toMap(MessageCountsView::getMessageId, Function.identity()));
        var currentVotes = messageVoteRepository.findAllByMessageIdInAndVoterUserId(messageIds, currentUserId).stream()
                .collect(Collectors.toMap(vote -> vote.getMessage().getId(), MessageVoteEntity::getVoteType));
        var labels = messageRepository.findLabelsByMessageIds(messageIds).stream()
                .collect(Collectors.groupingBy(
                        MessageLabelView::getMessageId,
                        Collectors.mapping(MessageLabelView::getLabel, Collectors.toCollection(LinkedHashSet::new))));
        var userNames = getUserNames(messages);
        return PagedResult.from(page).map(message -> {
            var messageCounts = counts.get(message.getId());
            var vote = currentVotes.get(message.getId());
            return new MessageDto(
                    message.getId(),
                    message.isAnonymous() ? "Anonymous" : getUserName(message.getCreatorUserId(), userNames),
                    message.getStatus() == MessageStatus.DELETED ? DELETED_CONTENT : message.getContent(),
                    message.getCreatedAt(),
                    messageCounts == null ? 0 : messageCounts.getUpvotes(),
                    messageCounts == null ? 0 : messageCounts.getDownvotes(),
                    messageCounts == null ? 0 : messageCounts.getReplies(),
                    vote == null ? null : vote.name(),
                    message.getStatus() == MessageStatus.DELETED,
                    message.getStatus() != MessageStatus.DELETED
                            && !message.getCreatorUserId().equals(currentUserId),
                    labels.getOrDefault(message.getId(), new LinkedHashSet<>()),
                    message.getSentiment());
        });
    }

    private Map<Long, String> getUserNames(List<MessageEntity> messages) {
        return usersAPI.findNamesByIds(messages.stream()
                .filter(message -> !message.isAnonymous())
                .map(MessageEntity::getCreatorUserId)
                .collect(Collectors.toSet()));
    }

    private Map<Long, String> getReplyUserNames(List<ReplyEntity> replies) {
        return usersAPI.findNamesByIds(replies.stream()
                .filter(reply -> !reply.isAnonymous())
                .map(ReplyEntity::getCreatorUserId)
                .collect(Collectors.toSet()));
    }

    private String getUserName(Long userId, Map<Long, String> userNames) {
        var userName = userNames.get(userId);
        if (userName == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return userName;
    }

    @Transactional(readOnly = true)
    public MessageDetailsDto findMessage(Long messageId, Long currentUserId) {
        var message = messageRepository
                .findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        var deleted = message.getStatus() == MessageStatus.DELETED;
        var ownedByCurrentUser = message.getCreatorUserId().equals(currentUserId);
        var currentUserVote = messageVoteRepository
                .findByMessageIdAndVoterUserId(messageId, currentUserId)
                .map(MessageVoteEntity::getVoteType)
                .map(Enum::name)
                .orElse(null);
        return new MessageDetailsDto(
                message.getId(),
                message.isAnonymous() ? "Anonymous" : getUserName(message.getCreatorUserId()),
                deleted ? DELETED_CONTENT : message.getContent(),
                message.getCreatedAt(),
                messageVoteRepository.countByMessageIdAndVoteType(messageId, VoteType.UPVOTE),
                messageVoteRepository.countByMessageIdAndVoteType(messageId, VoteType.DOWNVOTE),
                replyRepository.countByMessageIdAndStatus(messageId, ReplyStatus.ACTIVE),
                currentUserVote,
                deleted,
                !deleted && ownedByCurrentUser,
                !deleted && !ownedByCurrentUser,
                new LinkedHashSet<>(message.getLabels()),
                message.getSentiment());
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
        reply.setCreatorUserId(cmd.creatorId());
        reply.setAnonymous(cmd.anonymous());
        replyRepository.save(reply);
        eventPublisher.publishEvent(new ReplyCreatedEvent(reply.getId(), reply.getContent()));
    }

    @Transactional(readOnly = true)
    public List<ReplyDto> findReplies(Long messageId, Long currentUserId) {
        if (!messageRepository.existsById(messageId)) {
            throw new ResourceNotFoundException("Message not found");
        }
        var replies = replyRepository.findAllByMessageIdOrderByCreatedAtAsc(messageId);
        var userNames = getReplyUserNames(replies);
        return replies.stream()
                .map(reply -> {
                    var deleted = reply.getStatus() == ReplyStatus.DELETED;
                    return new ReplyDto(
                            reply.getId(),
                            reply.isAnonymous() ? "Anonymous" : getUserName(reply.getCreatorUserId(), userNames),
                            deleted ? DELETED_REPLY_CONTENT : reply.getContent(),
                            reply.getCreatedAt(),
                            reply.getUpdatedAt(),
                            replyVoteRepository.countByReplyIdAndVoteType(reply.getId(), VoteType.UPVOTE),
                            replyVoteRepository.countByReplyIdAndVoteType(reply.getId(), VoteType.DOWNVOTE),
                            replyVoteRepository
                                    .findByReplyIdAndVoterUserId(reply.getId(), currentUserId)
                                    .map(ReplyVoteEntity::getVoteType)
                                    .map(Enum::name)
                                    .orElse(null),
                            deleted,
                            !deleted && reply.getCreatorUserId().equals(currentUserId),
                            !deleted && !reply.getCreatorUserId().equals(currentUserId));
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
                .findByMessageIdAndVoterUserId(messageId, currentUserId)
                .orElseGet(() -> {
                    var newVote = new MessageVoteEntity();
                    newVote.setMessage(message);
                    newVote.setVoterUserId(currentUserId);
                    return newVote;
                });
        vote.setVoteType(voteType);
        messageVoteRepository.save(vote);
    }

    @Transactional
    public void removeMessageVote(Long messageId, Long currentUserId) {
        getVotableMessage(messageId, currentUserId);
        messageVoteRepository
                .findByMessageIdAndVoterUserId(messageId, currentUserId)
                .ifPresent(messageVoteRepository::delete);
    }

    @Transactional
    public void voteOnReply(Long messageId, Long replyId, Long currentUserId, VoteType voteType) {
        var reply = getVotableReply(messageId, replyId, currentUserId);
        var vote = replyVoteRepository
                .findByReplyIdAndVoterUserId(replyId, currentUserId)
                .orElseGet(() -> {
                    var newVote = new ReplyVoteEntity();
                    newVote.setReply(reply);
                    newVote.setVoterUserId(currentUserId);
                    return newVote;
                });
        vote.setVoteType(voteType);
        replyVoteRepository.save(vote);
    }

    @Transactional
    public void removeReplyVote(Long messageId, Long replyId, Long currentUserId) {
        getVotableReply(messageId, replyId, currentUserId);
        replyVoteRepository.findByReplyIdAndVoterUserId(replyId, currentUserId).ifPresent(replyVoteRepository::delete);
    }

    private ReplyEntity getVotableReply(Long messageId, Long replyId, Long currentUserId) {
        var reply = replyRepository
                .findById(replyId)
                .filter(candidate -> candidate.getMessage().getId().equals(messageId))
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found"));
        if (reply.getStatus() == ReplyStatus.DELETED) {
            throw new AccessDeniedException("Deleted replies cannot be voted on");
        }
        if (reply.getCreatorUserId().equals(currentUserId)) {
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
        if (message.getCreatorUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You cannot vote on your own message");
        }
        return message;
    }

    private ReplyEntity getEditableReply(Long messageId, Long replyId, Long currentUserId) {
        var reply = replyRepository
                .findById(replyId)
                .filter(candidate -> candidate.getMessage().getId().equals(messageId))
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found"));
        if (!reply.getCreatorUserId().equals(currentUserId)) {
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
        if (!message.getCreatorUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own messages");
        }
        if (message.getStatus() == MessageStatus.DELETED) {
            throw new AccessDeniedException("Deleted messages cannot be edited");
        }
        return message;
    }

    private String getUserName(Long userId) {
        return usersAPI.findNameById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
