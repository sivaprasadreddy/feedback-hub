package dev.sivalabs.feedbackhub.users;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserEntity> findAllByOrderByCreatedAtDesc();

    @Query("""
            select u from UserEntity u
            where (:role is null or u.role = :role)
              and (:active is null or u.active = :active)
            order by u.createdAt desc
            """)
    List<UserEntity> findUsers(@Param("role") Role role, @Param("active") Boolean active);
}
