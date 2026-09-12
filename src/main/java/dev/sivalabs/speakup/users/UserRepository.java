package dev.sivalabs.speakup.users;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @Modifying
    @Query("update UserEntity u set u.name = :name where u.id = :id")
    void updateUser(Long id, String name);
}
