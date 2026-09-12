package dev.sivalabs.speakup.users;

import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserService {
    static final String INITIAL_PASSWORD = "secret123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id).map(this::toUserDto);
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toUserDto)
                .toList();
    }

    @Transactional
    public void createUser(CreateUserCmd cmd) {
        if (userRepository.existsByEmailIgnoreCase(cmd.email())) {
            throw new DuplicateEmailException();
        }
        var user = new UserEntity();
        user.setName(cmd.name());
        user.setEmail(cmd.email());
        user.setPassword(passwordEncoder.encode(INITIAL_PASSWORD));
        user.setRole(cmd.role());
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void updateUser(Long userId, UpdateUserCmd cmd) {
        var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        userRepository.updateUser(user.getId(), cmd.name());
    }

    @Transactional
    public void editUser(Long actorId, Long userId, EditUserCmd cmd) {
        if (actorId.equals(userId)) {
            throw new AccessDeniedException("You cannot edit your own account");
        }
        var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setRole(cmd.role());
        user.setActive(cmd.active());
    }

    private UserDto toUserDto(UserEntity user) {
        return new UserDto(
                user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
