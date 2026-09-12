package dev.sivalabs.speakup.messages;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    @EntityGraph(attributePaths = "creator")
    List<MessageEntity> findAllByOrderByCreatedAtDesc();

    Page<MessageEntity> findPageByOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = """
                    select m.*
                    from messages m
                    left join message_votes v on v.message_id = m.id and v.vote_type = 'UPVOTE'
                    group by m.id
                    order by count(v.id) desc, m.created_at desc
                    """, countQuery = "select count(*) from messages", nativeQuery = true)
    Page<MessageEntity> findAllByPopularity(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "creator")
    Optional<MessageEntity> findById(Long id);
}
