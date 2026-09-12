package dev.sivalabs.speakup.messages;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    @EntityGraph(attributePaths = "creator")
    List<MessageEntity> findAllByOrderByCreatedAtDesc();
}
