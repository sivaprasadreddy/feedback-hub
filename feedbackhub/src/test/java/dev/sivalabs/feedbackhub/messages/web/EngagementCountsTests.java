package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import dev.sivalabs.feedbackhub.messages.domain.models.CreateReplyCmd;
import dev.sivalabs.feedbackhub.messages.domain.models.VoteType;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

class EngagementCountsTests extends BaseIT {
    // seeded: message 4004 (zero counts, no replies), reply 4101 on message 4101 (zero counts),
    // message 4006 with an upvote by siva (vote 4002) whose upvote_count is 0
    private static final long EMPTY_MESSAGE_ID = 4004L;
    private static final long REPLY_ID = 4101L;
    private static final long VOTED_MESSAGE_ID = 4006L;

    @Autowired
    MessageService messageService;

    @Autowired
    DataSource dataSource;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void countersDefaultToZeroAndCannotBecomeNegative() {
        assertThat(jdbcClient
                        .sql("select upvote_count, downvote_count, reply_count from messages where id = :id")
                        .param("id", EMPTY_MESSAGE_ID)
                        .query()
                        .singleRow())
                .containsEntry("upvote_count", 0L)
                .containsEntry("downvote_count", 0L)
                .containsEntry("reply_count", 0L);
        assertThat(jdbcClient
                        .sql("select upvote_count, downvote_count from replies where id = :id")
                        .param("id", REPLY_ID)
                        .query()
                        .singleRow())
                .containsEntry("upvote_count", 0L)
                .containsEntry("downvote_count", 0L);

        var jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update("update messages set upvote_count = -1 where id = ?", EMPTY_MESSAGE_ID))
                .hasMessageContaining("message_upvote_count_non_negative");
        assertThatThrownBy(() -> jdbc.update("update replies set downvote_count = -1 where id = ?", REPLY_ID))
                .hasMessageContaining("reply_downvote_count_non_negative");
    }

    @Test
    void concurrentVotesAndRepliesDoNotLoseCounterUpdates() throws Exception {
        runConcurrently(
                () -> messageService.voteOnMessage(EMPTY_MESSAGE_ID, 1L, VoteType.UPVOTE),
                () -> messageService.voteOnMessage(EMPTY_MESSAGE_ID, 3L, VoteType.UPVOTE));
        runConcurrently(
                () -> messageService.createReply(
                        new CreateReplyCmd(EMPTY_MESSAGE_ID, "First concurrent reply", 1L, false)),
                () -> messageService.createReply(
                        new CreateReplyCmd(EMPTY_MESSAGE_ID, "Second concurrent reply", 3L, false)));

        var counts = jdbcClient
                .sql("select upvote_count, downvote_count, reply_count from messages where id = :id")
                .param("id", EMPTY_MESSAGE_ID)
                .query()
                .singleRow();
        assertThat(counts)
                .containsEntry("upvote_count", 2L)
                .containsEntry("downvote_count", 0L)
                .containsEntry("reply_count", 2L);
        assertThat(messageVoteCount(EMPTY_MESSAGE_ID, 1L)).isEqualTo(1);
        assertThat(messageVoteCount(EMPTY_MESSAGE_ID, 3L)).isEqualTo(1);
        assertThat(jdbcClient
                        .sql("select count(*) from replies where message_id = :id")
                        .param("id", EMPTY_MESSAGE_ID)
                        .query(Long.class)
                        .single())
                .isEqualTo(2);
    }

    @Test
    void voteRemovalRollsBackWhenItsCounterCannotBeUpdated() {
        assertThat(messageVoteCount(VOTED_MESSAGE_ID, 2L)).isOne();

        assertThatThrownBy(() -> messageService.removeMessageVote(VOTED_MESSAGE_ID, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to update engagement counts");

        assertThat(messageVoteCount(VOTED_MESSAGE_ID, 2L)).isOne();
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

    private long messageVoteCount(long messageId, long userId) {
        return jdbcClient
                .sql("select count(*) from message_votes where message_id = :messageId and user_id = :userId")
                .param("messageId", messageId)
                .param("userId", userId)
                .query(Long.class)
                .single();
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
