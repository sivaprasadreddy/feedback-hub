package dev.sivalabs.feedbackhub.messages;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReplyVoteRepository extends JpaRepository<ReplyVoteEntity, Long> {
    Optional<ReplyVoteEntity> findByReplyIdAndVoterUserId(Long replyId, Long voterUserId);
}
