package dev.sivalabs.feedbackhub.messages;

import dev.sivalabs.feedbackhub.ApplicationProperties;
import dev.sivalabs.feedbackhub.shared.PagedResult;
import dev.sivalabs.feedbackhub.shared.ResourceNotFoundException;
import dev.sivalabs.feedbackhub.users.UsersAPI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MessageService {
    private static final int MAX_TOPICS = 3;
    private static final Instant EARLIEST_ANALYSIS_DATE = Instant.parse("0001-01-01T00:00:00Z");
    private static final Instant LATEST_ANALYSIS_DATE = Instant.parse("9999-12-31T23:59:59Z");
    static final String DELETED_CONTENT = "This message has been deleted.";
    static final String DELETED_REPLY_CONTENT = "This reply has been deleted.";
    private final MessageRepository messageRepository;
    private final ReplyRepository replyRepository;
    private final MessageVoteRepository messageVoteRepository;
    private final ReplyVoteRepository replyVoteRepository;
    private final UsersAPI usersAPI;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageAnalyzer messageAnalyzer;
    private final ApplicationProperties properties;

    MessageService(
            MessageRepository messageRepository,
            ReplyRepository replyRepository,
            MessageVoteRepository messageVoteRepository,
            ReplyVoteRepository replyVoteRepository,
            UsersAPI usersAPI,
            ApplicationEventPublisher eventPublisher,
            MessageAnalyzer messageAnalyzer,
            ApplicationProperties properties) {
        this.messageRepository = messageRepository;
        this.replyRepository = replyRepository;
        this.messageVoteRepository = messageVoteRepository;
        this.replyVoteRepository = replyVoteRepository;
        this.usersAPI = usersAPI;
        this.eventPublisher = eventPublisher;
        this.messageAnalyzer = messageAnalyzer;
        this.properties = properties;
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
        PageRequest pageable = feedPageRequest(pageNo);
        Page<MessageEntity> pagedMessages = messageRepository.findPageByOrderByCreatedAtDesc(pageable);
        return toMessagePage(pagedMessages, currentUserId);
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
                        message.getStatus() == MessageStatus.DELETED,
                        new LinkedHashSet<>(message.getTopics()),
                        message.getSentiment()));
    }

    @Transactional(readOnly = true)
    public AdminMessageDto findMessageForAdmin(Long messageId) {
        var message = messageRepository
                .findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        return new AdminMessageDto(
                message.getId(),
                message.isAnonymous() ? "Anonymous" : getUserName(message.getCreatorUserId()),
                message.getStatus() == MessageStatus.DELETED ? DELETED_CONTENT : message.getContent(),
                message.getCreatedAt(),
                message.getStatus() == MessageStatus.DELETED,
                new LinkedHashSet<>(message.getTopics()),
                message.getSentiment());
    }

    @Transactional(readOnly = true)
    public MessageExportData findMessageExportData(Long messageId) {
        var message = findMessageForAdmin(messageId);
        var replies = replyRepository.findAllByMessageIdOrderByCreatedAtAsc(messageId);
        var userNames = getReplyUserNames(replies);
        var replyDtos = replies.stream()
                .map(reply -> new AdminReplyDto(
                        reply.getId(),
                        messageId,
                        reply.isAnonymous() ? "Anonymous" : getUserName(reply.getCreatorUserId(), userNames),
                        reply.getStatus() == ReplyStatus.DELETED ? DELETED_REPLY_CONTENT : reply.getContent(),
                        reply.getCreatedAt(),
                        reply.getStatus() == ReplyStatus.DELETED,
                        reply.isSpam()))
                .toList();
        return new MessageExportData(message, replyDtos);
    }

    @Transactional
    public void analyzeMessage(Long messageId) {
        var message = messageRepository
                .findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (message.getSentiment() != null) {
            return;
        }
        var analysis = messageAnalyzer.analyze(message.getContent());
        if (analysis == null || analysis.sentiment() == null) {
            throw new IllegalStateException("Message analysis did not return a sentiment");
        }
        var topics = new LinkedHashSet<String>();
        if (analysis.topics() != null) {
            analysis.topics().stream()
                    .filter(java.util.Objects::nonNull)
                    .map(MessageTopic::displayName)
                    .limit(MAX_TOPICS)
                    .forEach(topics::add);
        }
        if (topics.isEmpty()) {
            topics.add(MessageTopic.OTHER.displayName());
        }
        message.setTopics(topics);
        message.setSentiment(analysis.sentiment());
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
        updateMessageCounts(reply.getMessage().getId(), 0, 0, -1);
    }

    private PageRequest feedPageRequest(int pageNo) {
        return PageRequest.of(pageNo - 1, properties.feedPageSize());
    }

    private PageRequest adminPageRequest(int pageNo) {
        return PageRequest.of(pageNo - 1, properties.adminPageSize());
    }

    private MessageDto toMessageDto(MessageEntity message, Long currentUserId) {
        return new MessageDto(
                message.getId(),
                message.isAnonymous() ? "Anonymous" : getUserName(message.getCreatorUserId()),
                message.getStatus() == MessageStatus.DELETED ? DELETED_CONTENT : message.getContent(),
                message.getCreatedAt(),
                message.getUpvoteCount(),
                message.getDownvoteCount(),
                message.getReplyCount(),
                messageVoteRepository
                        .findByMessageIdAndVoterUserId(message.getId(), currentUserId)
                        .map(MessageVoteEntity::getVoteType)
                        .map(Enum::name)
                        .orElse(null),
                message.getStatus() == MessageStatus.DELETED,
                message.getStatus() != MessageStatus.DELETED
                        && message.getCreatorUserId().equals(currentUserId),
                message.getStatus() != MessageStatus.DELETED
                        && !message.getCreatorUserId().equals(currentUserId),
                new LinkedHashSet<>(message.getTopics()),
                message.getSentiment());
    }

    private PagedResult<MessageDto> toMessagePage(Page<MessageEntity> page, Long currentUserId) {
        var messages = page.getContent();
        var messageIds = messages.stream().map(MessageEntity::getId).toList();
        if (messageIds.isEmpty()) {
            return PagedResult.from(page).map(message -> toMessageDto(message, currentUserId));
        }
        var currentVotes = messageVoteRepository.findAllByMessageIdInAndVoterUserId(messageIds, currentUserId).stream()
                .collect(Collectors.toMap(vote -> vote.getMessage().getId(), MessageVoteEntity::getVoteType));
        var topics = messageRepository.findTopicsByMessageIds(messageIds).stream()
                .collect(Collectors.groupingBy(
                        MessageTopicView::getMessageId,
                        Collectors.mapping(MessageTopicView::getTopic, Collectors.toCollection(LinkedHashSet::new))));
        var userNames = getUserNames(messages);
        return PagedResult.from(page).map(message -> {
            var vote = currentVotes.get(message.getId());
            return new MessageDto(
                    message.getId(),
                    message.isAnonymous() ? "Anonymous" : getUserName(message.getCreatorUserId(), userNames),
                    message.getStatus() == MessageStatus.DELETED ? DELETED_CONTENT : message.getContent(),
                    message.getCreatedAt(),
                    message.getUpvoteCount(),
                    message.getDownvoteCount(),
                    message.getReplyCount(),
                    vote == null ? null : vote.name(),
                    message.getStatus() == MessageStatus.DELETED,
                    message.getStatus() != MessageStatus.DELETED
                            && message.getCreatorUserId().equals(currentUserId),
                    message.getStatus() != MessageStatus.DELETED
                            && !message.getCreatorUserId().equals(currentUserId),
                    topics.getOrDefault(message.getId(), new LinkedHashSet<>()),
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
    public MessageDto findMessage(Long messageId, Long currentUserId) {
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
        return new MessageDto(
                message.getId(),
                message.isAnonymous() ? "Anonymous" : getUserName(message.getCreatorUserId()),
                deleted ? DELETED_CONTENT : message.getContent(),
                message.getCreatedAt(),
                message.getUpvoteCount(),
                message.getDownvoteCount(),
                message.getReplyCount(),
                currentUserVote,
                deleted,
                !deleted && ownedByCurrentUser,
                !deleted && !ownedByCurrentUser,
                new LinkedHashSet<>(message.getTopics()),
                message.getSentiment());
    }

    @Transactional(readOnly = true)
    public String getMessageContent(Long messageId, Long currentUserId) {
        var message = getEditableMessage(messageId, currentUserId);
        return message.getContent();
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
        updateMessageCounts(message.getId(), 0, 0, 1);
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
                            reply.getUpvoteCount(),
                            reply.getDownvoteCount(),
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
    public String getReplyContent(Long messageId, Long replyId, Long currentUserId) {
        var reply = getEditableReply(messageId, replyId, currentUserId);
        return reply.getContent();
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
        updateMessageCounts(messageId, 0, 0, -1);
    }

    @Transactional
    public void voteOnMessage(Long messageId, Long currentUserId, VoteType voteType) {
        var message = getVotableMessage(messageId, currentUserId);
        var existingVote = messageVoteRepository.findByMessageIdAndVoterUserId(messageId, currentUserId);
        var previousVoteType = existingVote.map(MessageVoteEntity::getVoteType).orElse(null);
        var vote = existingVote.orElseGet(() -> {
            var newVote = new MessageVoteEntity();
            newVote.setMessage(message);
            newVote.setVoterUserId(currentUserId);
            return newVote;
        });
        vote.setVoteType(voteType);
        messageVoteRepository.save(vote);
        updateMessageVoteCounts(messageId, previousVoteType, voteType);
    }

    @Transactional
    public void removeMessageVote(Long messageId, Long currentUserId) {
        getVotableMessage(messageId, currentUserId);
        messageVoteRepository
                .findByMessageIdAndVoterUserId(messageId, currentUserId)
                .ifPresent(vote -> {
                    messageVoteRepository.delete(vote);
                    updateMessageVoteCounts(messageId, vote.getVoteType(), null);
                });
    }

    @Transactional
    public void voteOnReply(Long messageId, Long replyId, Long currentUserId, VoteType voteType) {
        var reply = getVotableReply(messageId, replyId, currentUserId);
        var existingVote = replyVoteRepository.findByReplyIdAndVoterUserId(replyId, currentUserId);
        var previousVoteType = existingVote.map(ReplyVoteEntity::getVoteType).orElse(null);
        var vote = existingVote.orElseGet(() -> {
            var newVote = new ReplyVoteEntity();
            newVote.setReply(reply);
            newVote.setVoterUserId(currentUserId);
            return newVote;
        });
        vote.setVoteType(voteType);
        replyVoteRepository.save(vote);
        updateReplyVoteCounts(replyId, previousVoteType, voteType);
    }

    @Transactional
    public void removeReplyVote(Long messageId, Long replyId, Long currentUserId) {
        getVotableReply(messageId, replyId, currentUserId);
        replyVoteRepository.findByReplyIdAndVoterUserId(replyId, currentUserId).ifPresent(vote -> {
            replyVoteRepository.delete(vote);
            updateReplyVoteCounts(replyId, vote.getVoteType(), null);
        });
    }

    private void updateMessageVoteCounts(Long messageId, VoteType previousVoteType, VoteType newVoteType) {
        updateMessageCounts(
                messageId,
                voteDelta(previousVoteType, newVoteType, VoteType.UPVOTE),
                voteDelta(previousVoteType, newVoteType, VoteType.DOWNVOTE),
                0);
    }

    private void updateReplyVoteCounts(Long replyId, VoteType previousVoteType, VoteType newVoteType) {
        var upvoteDelta = voteDelta(previousVoteType, newVoteType, VoteType.UPVOTE);
        var downvoteDelta = voteDelta(previousVoteType, newVoteType, VoteType.DOWNVOTE);
        if (upvoteDelta == 0 && downvoteDelta == 0) {
            return;
        }
        if (replyRepository.updateVoteCounts(replyId, upvoteDelta, downvoteDelta) != 1) {
            throw new IllegalStateException("Failed to update engagement counts for reply " + replyId);
        }
    }

    private void updateMessageCounts(Long messageId, int upvoteDelta, int downvoteDelta, int replyDelta) {
        if (upvoteDelta == 0 && downvoteDelta == 0 && replyDelta == 0) {
            return;
        }
        if (messageRepository.updateCounts(messageId, upvoteDelta, downvoteDelta, replyDelta) != 1) {
            throw new IllegalStateException("Failed to update engagement counts for message " + messageId);
        }
    }

    private int voteDelta(VoteType previousVoteType, VoteType newVoteType, VoteType countedType) {
        return (newVoteType == countedType ? 1 : 0) - (previousVoteType == countedType ? 1 : 0);
    }

    @Transactional(readOnly = true)
    public MessageStatistics getStatistics() {
        return new MessageStatistics(messageRepository.count(), replyRepository.count());
    }

    @Transactional(readOnly = true)
    public List<SentimentCount> getSentimentCounts(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be on or before end date.");
        }

        var zone = ZoneId.systemDefault();
        var start = startDate == null
                ? EARLIEST_ANALYSIS_DATE
                : startDate.atStartOfDay(zone).toInstant();
        var endExclusive = endDate == null
                ? LATEST_ANALYSIS_DATE
                : endDate.plusDays(1).atStartOfDay(zone).toInstant();
        var counts = new EnumMap<MessageSentiment, Long>(MessageSentiment.class);
        messageRepository
                .countBySentimentBetween(start, endExclusive)
                .forEach(result -> counts.put(result.getSentiment(), result.getCount()));
        var maximum = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);

        return java.util.Arrays.stream(MessageSentiment.values())
                .map(sentiment -> {
                    var count = counts.getOrDefault(sentiment, 0L);
                    var percentage = maximum == 0 ? 0 : (int) Math.round(count * 100.0 / maximum);
                    return new SentimentCount(sentiment, count, percentage);
                })
                .toList();
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
