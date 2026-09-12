package dev.sivalabs.speakup.messages;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    @EntityGraph(attributePaths = "creator")
    List<MessageEntity> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = "creator")
    Optional<MessageEntity> findById(Long id);
}
