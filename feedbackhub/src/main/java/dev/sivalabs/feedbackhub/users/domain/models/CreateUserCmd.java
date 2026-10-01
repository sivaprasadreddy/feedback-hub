package dev.sivalabs.feedbackhub.users.domain.models;

public record CreateUserCmd(String name, String email, Role role) {}
