package dev.sivalabs.speakup.messages;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReplyRepository extends JpaRepository<ReplyEntity, Long> {
    long countByMessageId(Long messageId);

    @EntityGraph(attributePaths = {"message", "creator"})
    List<ReplyEntity> findAllByMessageId(Long messageId);
}
