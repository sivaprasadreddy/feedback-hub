package dev.sivalabs.feedbackhub.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.sivalabs.feedbackhub.BaseIT;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class EngagementCountsTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ReplyRepository replyRepository;

    @Autowired
    MessageVoteRepository messageVoteRepository;

    @Autowired
    MessageService messageService;

    @Autowired
    DataSource dataSource;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void countersDefaultToZeroAndCannotBecomeNegative() {
        var message = createMessage(1L);
        var reply = createReply(message, 2L);

        assertThat(messageRepository.findById(message.getId()).orElseThrow())
                .extracting(
                        MessageEntity::getUpvoteCount, MessageEntity::getDownvoteCount, MessageEntity::getReplyCount)
                .containsExactly(0L, 0L, 0L);
        assertThat(replyRepository.findById(reply.getId()).orElseThrow())
                .extracting(ReplyEntity::getUpvoteCount, ReplyEntity::getDownvoteCount)
                .containsExactly(0L, 0L);

        var jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update("update messages set upvote_count = -1 where id = ?", message.getId()))
                .hasMessageContaining("message_upvote_count_non_negative");
        assertThatThrownBy(() -> jdbc.update("update replies set downvote_count = -1 where id = ?", reply.getId()))
                .hasMessageContaining("reply_downvote_count_non_negative");
    }

    @Test
    void concurrentVotesAndRepliesDoNotLoseCounterUpdates() throws Exception {
        var message = createMessage(2L);

        runConcurrently(
                () -> messageService.voteOnMessage(message.getId(), 1L, VoteType.UPVOTE),
                () -> messageService.voteOnMessage(message.getId(), 3L, VoteType.UPVOTE));
        runConcurrently(
                () -> messageService.createReply(
                        new CreateReplyCmd(message.getId(), "First concurrent reply", 1L, false)),
                () -> messageService.createReply(
                        new CreateReplyCmd(message.getId(), "Second concurrent reply", 3L, false)));

        var updated = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(updated.getUpvoteCount()).isEqualTo(2);
        assertThat(updated.getDownvoteCount()).isZero();
        assertThat(updated.getReplyCount()).isEqualTo(2);
        assertThat(messageVoteRepository.findAllByMessageIdInAndVoterUserId(List.of(message.getId()), 1L))
                .hasSize(1);
        assertThat(messageVoteRepository.findAllByMessageIdInAndVoterUserId(List.of(message.getId()), 3L))
                .hasSize(1);
        assertThat(replyRepository.findAllByMessageIdOrderByCreatedAtAsc(message.getId()))
                .hasSize(2);
    }

    @Test
    void voteRemovalRollsBackWhenItsCounterCannotBeUpdated() {
        var message = createMessage(1L);
        messageService.voteOnMessage(message.getId(), 2L, VoteType.UPVOTE);
        var jdbc = new JdbcTemplate(dataSource);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> assertThat(messageRepository.updateCounts(message.getId(), -1, 0, 0))
                        .isOne());
        assertThat(jdbc.queryForObject("select upvote_count from messages where id = ?", Long.class, message.getId()))
                .isZero();
        assertThat(messageVoteRepository.findByMessageIdAndVoterUserId(message.getId(), 2L))
                .isPresent();

        assertThatThrownBy(() -> messageService.removeMessageVote(message.getId(), 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to update engagement counts");

        assertThat(messageVoteRepository.findByMessageIdAndVoterUserId(message.getId(), 2L))
                .isPresent()
                .get()
                .extracting(MessageVoteEntity::getVoteType)
                .isEqualTo(VoteType.UPVOTE);
    }

    @Test
    void migrationBackfillsExistingEngagementData() {
        var schema = "engagement_counts_" + UUID.randomUUID().toString().replace("-", "");
        var jdbc = new JdbcTemplate(dataSource);
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .target("12")
                    .load()
                    .migrate();
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .load()
                    .migrate();

            assertThat(jdbc.queryForMap("select upvote_count, downvote_count, reply_count from " + schema
                            + ".messages where id = 201"))
                    .containsEntry("upvote_count", 1L)
                    .containsEntry("downvote_count", 0L)
                    .containsEntry("reply_count", 2L);
            assertThat(jdbc.queryForMap(
                            "select upvote_count, downvote_count from " + schema + ".replies where id = 301"))
                    .containsEntry("upvote_count", 1L)
                    .containsEntry("downvote_count", 0L);
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }

    private MessageEntity createMessage(Long creatorUserId) {
        var message = new MessageEntity();
        message.setContent("Engagement counts " + UUID.randomUUID());
        message.setCreatorUserId(creatorUserId);
        return messageRepository.saveAndFlush(message);
    }

    private ReplyEntity createReply(MessageEntity message, Long creatorUserId) {
        var reply = new ReplyEntity();
        reply.setMessage(message);
        reply.setContent("Engagement count reply " + UUID.randomUUID());
        reply.setCreatorUserId(creatorUserId);
        return replyRepository.saveAndFlush(reply);
    }

    private void runConcurrently(Runnable first, Runnable second) throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> {
                await(start);
                first.run();
            });
            var secondResult = executor.submit(() -> {
                await(start);
                second.run();
            });
            start.countDown();
            firstResult.get();
            secondResult.get();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to start concurrent operation", exception);
        }
    }
}
