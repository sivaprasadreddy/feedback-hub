package dev.sivalabs.feedbackhub.messages;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReplyRepository extends JpaRepository<ReplyEntity, Long> {
    long countByMessageIdAndStatus(Long messageId, ReplyStatus status);

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
