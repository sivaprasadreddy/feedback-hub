package dev.sivalabs.feedbackhub.messages;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface MessageVoteRepository extends JpaRepository<MessageVoteEntity, Long> {
    Optional<MessageVoteEntity> findByMessageIdAndVoterUserId(Long messageId, Long voterUserId);

    List<MessageVoteEntity> findAllByMessageIdInAndVoterUserId(List<Long> messageIds, Long voterUserId);
}
