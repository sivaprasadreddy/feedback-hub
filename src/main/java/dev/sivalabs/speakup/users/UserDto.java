package dev.sivalabs.speakup.users;

import java.time.Instant;

public record UserDto(Long id, String tenantId, String name, String email, Role role, Instant createdAt) {}
