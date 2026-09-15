package dev.sivalabs.feedbackhub.users;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserService {
    static final String INITIAL_PASSWORD = "secret123";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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
    public List<UserDto> findByIds(Set<Long> ids) {
        return userRepository.findAllById(ids).stream().map(this::toUserDto).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> findUsers(UserFilterQuery query) {
        return userRepository.findUsers(query.role(), query.active()).stream()
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
    public ImportUsersResult importUsers(String csv) {
        final List<CsvRecord> records;
        try {
            records = CsvParser.parse(csv);
        } catch (IllegalArgumentException ex) {
            return new ImportUsersResult(0, List.of(new ImportUserError(1, ex.getMessage())));
        }

        if (records.isEmpty()) {
            return new ImportUsersResult(0, List.of(new ImportUserError(1, "CSV file is empty")));
        }

        var header = records.getFirst().values();
        if (header.size() != 3
                || !header.get(0).strip().equalsIgnoreCase("name")
                || !header.get(1).strip().equalsIgnoreCase("email")
                || !header.get(2).strip().equalsIgnoreCase("role")) {
            return new ImportUsersResult(0, List.of(new ImportUserError(1, "Header must be: name,email,role")));
        }

        var commands = new ArrayList<CreateUserCmd>();
        var errors = new ArrayList<ImportUserError>();
        var emailsInFile = new HashSet<String>();
        for (var record : records.subList(1, records.size())) {
            if (record.values().stream().allMatch(String::isBlank)) {
                continue;
            }
            if (record.values().size() != 3) {
                errors.add(new ImportUserError(record.rowNumber(), "Expected 3 columns"));
                continue;
            }

            var name = record.values().get(0).strip();
            var email = record.values().get(1).strip();
            var roleValue = record.values().get(2).strip().toUpperCase(Locale.ROOT);
            if (name.isBlank()) {
                errors.add(new ImportUserError(record.rowNumber(), "Name is required"));
            }
            if (email.isBlank()) {
                errors.add(new ImportUserError(record.rowNumber(), "Email is required"));
            } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                errors.add(new ImportUserError(record.rowNumber(), "Email address must be valid"));
            }

            Role role = null;
            try {
                role = Role.valueOf(roleValue.startsWith("ROLE_") ? roleValue : "ROLE_" + roleValue);
            } catch (IllegalArgumentException ex) {
                errors.add(new ImportUserError(record.rowNumber(), "Role must be ADMIN or USER"));
            }

            var normalizedEmail = email.toLowerCase(Locale.ROOT);
            if (!email.isBlank() && !emailsInFile.add(normalizedEmail)) {
                errors.add(new ImportUserError(record.rowNumber(), "Email is duplicated in the CSV file"));
            } else if (!email.isBlank() && userRepository.existsByEmailIgnoreCase(email)) {
                errors.add(new ImportUserError(record.rowNumber(), "Email address is already in use"));
            }
            if (name.isBlank()
                    || email.isBlank()
                    || !EMAIL_PATTERN.matcher(email).matches()
                    || role == null) {
                continue;
            }
            commands.add(new CreateUserCmd(name, email, role));
        }

        if (records.size() == 1 || (commands.isEmpty() && errors.isEmpty())) {
            errors.add(new ImportUserError(2, "CSV file does not contain any users"));
        }
        if (!errors.isEmpty()) {
            return new ImportUsersResult(0, List.copyOf(errors));
        }

        var users = commands.stream()
                .map(cmd -> {
                    var user = new UserEntity();
                    user.setName(cmd.name());
                    user.setEmail(cmd.email());
                    user.setPassword(passwordEncoder.encode(INITIAL_PASSWORD));
                    user.setRole(cmd.role());
                    user.setActive(true);
                    return user;
                })
                .toList();
        userRepository.saveAll(users);
        return new ImportUsersResult(users.size(), List.of());
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

    @Transactional
    public void changePassword(Long userId, ChangePasswordCmd cmd) {
        var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        if (!passwordEncoder.matches(cmd.currentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }
        user.setPassword(passwordEncoder.encode(cmd.newPassword()));
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    private UserDto toUserDto(UserEntity user) {
        return new UserDto(
                user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive(), user.getCreatedAt());
    }

    private record CsvRecord(int rowNumber, List<String> values) {}

    private static final class CsvParser {
        private CsvParser() {}

        static List<CsvRecord> parse(String source) {
            var csv = source.startsWith("\uFEFF") ? source.substring(1) : source;
            var records = new ArrayList<CsvRecord>();
            var values = new ArrayList<String>();
            var value = new StringBuilder();
            var quoted = false;
            var rowNumber = 1;
            var recordStart = 1;
            for (int index = 0; index < csv.length(); index++) {
                char current = csv.charAt(index);
                if (quoted) {
                    if (current == '"' && index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                        value.append('"');
                        index++;
                    } else if (current == '"') {
                        quoted = false;
                    } else {
                        value.append(current);
                        if (current == '\n') rowNumber++;
                    }
                } else if (current == '"' && value.isEmpty()) {
                    quoted = true;
                } else if (current == ',') {
                    values.add(value.toString());
                    value.setLength(0);
                } else if (current == '\n' || current == '\r') {
                    if (current == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') index++;
                    values.add(value.toString());
                    records.add(new CsvRecord(recordStart, List.copyOf(values)));
                    values.clear();
                    value.setLength(0);
                    rowNumber++;
                    recordStart = rowNumber;
                } else {
                    value.append(current);
                }
            }
            if (quoted) throw new IllegalArgumentException("CSV contains an unclosed quoted field");
            if (!value.isEmpty() || !values.isEmpty()) {
                values.add(value.toString());
                records.add(new CsvRecord(recordStart, List.copyOf(values)));
            }
            return records;
        }
    }
}
