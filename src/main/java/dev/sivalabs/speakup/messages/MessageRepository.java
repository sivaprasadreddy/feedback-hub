package dev.sivalabs.speakup.messages;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    @EntityGraph(attributePaths = "creator")
    List<MessageEntity> findAllByOrderByCreatedAtDesc();

    @Query(value = """
                    select m.*
                    from messages m
                    left join message_votes v on v.message_id = m.id and v.vote_type = 'UPVOTE'
                    group by m.id
                    order by count(v.id) desc, m.created_at desc
                    """, nativeQuery = true)
    List<MessageEntity> findAllByPopularity();

    @Override
    @EntityGraph(attributePaths = "creator")
    Optional<MessageEntity> findById(Long id);
}
