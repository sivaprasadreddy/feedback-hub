package dev.sivalabs.speakup.messages;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface MessageVoteRepository extends JpaRepository<MessageVoteEntity, Long> {
    Optional<MessageVoteEntity> findByMessageIdAndVoterId(Long messageId, Long voterId);

    long countByMessageIdAndVoteType(Long messageId, VoteType voteType);
}
