package dev.sivalabs.speakup.users;

import java.time.Instant;

public record UserDto(Long id, String name, String email, Role role, boolean active, Instant createdAt) {}
