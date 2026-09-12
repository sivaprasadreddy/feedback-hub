package dev.sivalabs.speakup.messages;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReplyVoteRepository extends JpaRepository<ReplyVoteEntity, Long> {
    Optional<ReplyVoteEntity> findByReplyIdAndVoterId(Long replyId, Long voterId);

    long countByReplyIdAndVoteType(Long replyId, VoteType voteType);
}
