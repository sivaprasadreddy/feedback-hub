package dev.sivalabs.feedbackhub.users.domain.models;

public record AccountDetails(String name, String email, Role role, boolean hasProfilePicture) {}
