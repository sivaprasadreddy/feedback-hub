package dev.sivalabs.feedbackhub.messages;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReplyRepository extends JpaRepository<ReplyEntity, Long> {
    long countByMessageIdAndStatus(Long messageId, ReplyStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update replies
            set upvote_count = upvote_count + :upvoteDelta,
                downvote_count = downvote_count + :downvoteDelta,
                version = version + 1
            where id = :replyId
              and upvote_count + :upvoteDelta >= 0
              and downvote_count + :downvoteDelta >= 0
            """, nativeQuery = true)
    int updateVoteCounts(
            @Param("replyId") Long replyId,
            @Param("upvoteDelta") int upvoteDelta,
            @Param("downvoteDelta") int downvoteDelta);

    @EntityGraph(attributePaths = "message")
    List<ReplyEntity> findAllByMessageIdOrderByCreatedAtAsc(Long messageId);

    @EntityGraph(attributePaths = "message")
    List<ReplyEntity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "message")
    Page<ReplyEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "message")
    @NonNull
    Optional<ReplyEntity> findById(@NonNull Long id);
}
