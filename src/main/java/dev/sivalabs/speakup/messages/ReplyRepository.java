package dev.sivalabs.speakup.messages;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReplyRepository extends JpaRepository<ReplyEntity, Long> {
    long countByMessageIdAndStatus(Long messageId, ReplyStatus status);

    @EntityGraph(attributePaths = {"message", "creator"})
    List<ReplyEntity> findAllByMessageIdOrderByCreatedAtAsc(Long messageId);
}
