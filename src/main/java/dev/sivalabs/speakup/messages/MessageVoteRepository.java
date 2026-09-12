package dev.sivalabs.speakup.messages;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface MessageVoteRepository extends JpaRepository<MessageVoteEntity, Long> {
    Optional<MessageVoteEntity> findByMessageIdAndVoterUserId(Long messageId, Long voterUserId);

    long countByMessageIdAndVoteType(Long messageId, VoteType voteType);

    List<MessageVoteEntity> findAllByMessageIdInAndVoterUserId(List<Long> messageIds, Long voterUserId);
}
