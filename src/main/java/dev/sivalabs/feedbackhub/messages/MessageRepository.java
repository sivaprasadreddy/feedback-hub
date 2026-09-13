package dev.sivalabs.feedbackhub.messages;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findAllByOrderByCreatedAtDesc();

    Page<MessageEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<MessageEntity> findPageByOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = """
                    select m.*
                    from messages m
                    left join message_votes v on v.message_id = m.id and v.vote_type = 'UPVOTE'
                    group by m.id
                    order by count(v.id) desc, m.created_at desc
                    """, countQuery = "select count(*) from messages", nativeQuery = true)
    Page<MessageEntity> findAllByPopularity(Pageable pageable);

    @Query("""
            select m.id as messageId,
                   count(distinct case when v.voteType = dev.sivalabs.feedbackhub.messages.VoteType.UPVOTE then v.id end) as upvotes,
                   count(distinct case when v.voteType = dev.sivalabs.feedbackhub.messages.VoteType.DOWNVOTE then v.id end) as downvotes,
                   count(distinct case when r.status = dev.sivalabs.feedbackhub.messages.ReplyStatus.ACTIVE then r.id end) as replies
            from MessageEntity m
            left join MessageVoteEntity v on v.message = m
            left join ReplyEntity r on r.message = m
            where m.id in :messageIds
            group by m.id
            """)
    List<MessageCountsView> findCountsByMessageIds(@Param("messageIds") List<Long> messageIds);

    @Query("""
            select m.id as messageId, topic as topic
            from MessageEntity m join m.topics topic
            where m.id in :messageIds
            """)
    List<MessageTopicView> findTopicsByMessageIds(@Param("messageIds") List<Long> messageIds);

    @Override
    Optional<MessageEntity> findById(Long id);
}

interface MessageCountsView {
    Long getMessageId();

    long getUpvotes();

    long getDownvotes();

    long getReplies();
}

interface MessageTopicView {
    Long getMessageId();

    String getTopic();
}
