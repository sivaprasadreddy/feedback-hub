package dev.sivalabs.feedbackhub.messages;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findAllByOrderByCreatedAtDesc();

    Page<MessageEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<MessageEntity> findPageByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select m from MessageEntity m order by m.upvoteCount desc, m.createdAt desc")
    Page<MessageEntity> findAllByPopularity(Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update messages
            set upvote_count = upvote_count + :upvoteDelta,
                downvote_count = downvote_count + :downvoteDelta,
                reply_count = reply_count + :replyDelta,
                version = version + 1
            where id = :messageId
              and upvote_count + :upvoteDelta >= 0
              and downvote_count + :downvoteDelta >= 0
              and reply_count + :replyDelta >= 0
            """, nativeQuery = true)
    int updateCounts(
            @Param("messageId") Long messageId,
            @Param("upvoteDelta") int upvoteDelta,
            @Param("downvoteDelta") int downvoteDelta,
            @Param("replyDelta") int replyDelta);

    @Query("""
            select m.id as messageId, topic as topic
            from MessageEntity m join m.topics topic
            where m.id in :messageIds
            """)
    List<MessageTopicView> findTopicsByMessageIds(@Param("messageIds") List<Long> messageIds);
}

interface MessageTopicView {
    Long getMessageId();

    String getTopic();
}
